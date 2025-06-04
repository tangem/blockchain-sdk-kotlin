package com.tangem.blockchain.blockchains.casper.network.provider

import com.squareup.moshi.adapter
import com.tangem.blockchain.blockchains.casper.models.CasperBalance
import com.tangem.blockchain.blockchains.casper.models.CasperTransaction
import com.tangem.blockchain.blockchains.casper.network.CasperApi
import com.tangem.blockchain.blockchains.casper.network.CasperNetworkProvider
import com.tangem.blockchain.blockchains.casper.network.request.CasperRpcBodyFactory
import com.tangem.blockchain.blockchains.casper.network.request.CasperTransactionBody
import com.tangem.blockchain.blockchains.casper.network.response.CasperRpcResponse
import com.tangem.blockchain.blockchains.casper.network.response.CasperRpcResponseResult
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.JsonRPCRequest
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.blockchain.network.moshi
import kotlinx.io.IOException
import okhttp3.Interceptor
import java.math.BigDecimal

@OptIn(ExperimentalStdlibApi::class)
internal class CasperRpcNetworkProvider(
    override val baseUrl: String,
    private val postfixUrl: String,
    headerInterceptors: List<Interceptor> = emptyList(),
    private val blockchain: Blockchain,
) : CasperNetworkProvider {

    private val api = createRetrofitInstance(baseUrl, headerInterceptors).create(CasperApi::class.java)

    override suspend fun getBalance(address: String): Result<CasperBalance> = post(
        body = CasperRpcBodyFactory.createQueryBalanceBody(address),
        onSuccess = { response: CasperRpcResponseResult.Balance ->
            CasperBalance(value = BigDecimal(response.balance).movePointLeft(blockchain.decimals()))
        },
        onFailure = {
            // Account is not funded yet
            if (it.code in ACCOUNT_NOT_FUNDED_ERROR_CODES) {
                Result.Success(CasperBalance(value = BigDecimal.ZERO))
            } else {
                Result.Failure(toDefaultError(it))
            }
        },
    )

    override suspend fun putDeploy(body: CasperTransactionBody): Result<CasperTransaction> = post(
        body = CasperRpcBodyFactory.createAccountPutDeployBody(body),
        onSuccess = { response: CasperRpcResponseResult.Deploy ->
            CasperTransaction(response.deployHash)
        },
        onFailure = {
            Result.Failure(toDefaultError(it))
        },
    )

    private suspend inline fun <reified Data, Domain> post(
        body: JsonRPCRequest,
        onSuccess: (Data) -> Domain,
        onFailure: (CasperRpcResponse.Failure) -> Result<Domain>,
    ): Result<Domain> {
        return try {
            when (val response = api.post(body = body, postfixUrl = postfixUrl)) {
                is CasperRpcResponse.Success -> {
                    runCatching {
                        moshi.adapter<Data>().fromJsonValue(response.result)
                    }.getOrNull()?.let { Result.Success(onSuccess(it)) } ?: Result.Failure(
                        BlockchainSdkError.UnsupportedOperation(
                            "Unknown Casper JSON-RPC response result",
                        ),
                    )
                }
                is CasperRpcResponse.Failure -> onFailure(response)
            }
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private fun toDefaultError(response: CasperRpcResponse.Failure): BlockchainSdkError {
        return IOException(response.message).toBlockchainSdkError()
    }

    private companion object {
        val ACCOUNT_NOT_FUNDED_ERROR_CODES = setOf(-32003, -32026)
    }
}