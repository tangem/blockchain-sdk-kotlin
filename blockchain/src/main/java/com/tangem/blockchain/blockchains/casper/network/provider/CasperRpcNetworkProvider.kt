package com.tangem.blockchain.blockchains.casper.network.provider

import com.squareup.moshi.adapter
import com.tangem.blockchain.blockchains.casper.models.CasperBalance
import com.tangem.blockchain.blockchains.casper.network.CasperApi
import com.tangem.blockchain.blockchains.casper.network.CasperNetworkProvider
import com.tangem.blockchain.blockchains.casper.network.request.CasperRpcBodyFactory
import com.tangem.blockchain.blockchains.casper.network.response.CasperRpcResponse
import com.tangem.blockchain.blockchains.casper.network.response.CasperRpcResponseResult
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.JsonRPCRequest
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
) : CasperNetworkProvider {

    private val api = createRetrofitInstance(baseUrl, headerInterceptors).create(CasperApi::class.java)

    override suspend fun getBalance(address: String): Result<CasperBalance> = post(
        body = CasperRpcBodyFactory.createQueryBalanceBody(address),
        onSuccess = { response: CasperRpcResponseResult.Balance ->
            CasperBalance(balance = BigDecimal(response.balance))
        },
        onFailure = { response ->
            if (response.isNoActorError()) BlockchainSdkError.AccountNotFound() else toDefaultError(response)
        },
    )

    private fun CasperRpcResponse.Failure.isNoActorError() = code == ERROR_CODE_QUERY_FAILED

    private suspend inline fun <reified Data, Domain> post(
        body: JsonRPCRequest,
        onSuccess: (Data) -> Domain,
        onFailure: (CasperRpcResponse.Failure) -> BlockchainSdkError = ::toDefaultError,
    ): Result<Domain> {
        return try {
            when (val response = api.post(body = body, postfixUrl = postfixUrl)) {
                is CasperRpcResponse.Success -> {
                    Result.Success(
                        data = onSuccess(
                            moshi.adapter<Data>().fromJsonValue(response.result)!!,
                        ),
                    )
                }
                is CasperRpcResponse.Failure -> Result.Failure(error = onFailure(response))
            }
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private fun toDefaultError(response: CasperRpcResponse.Failure): BlockchainSdkError {
        return IOException(response.message).toBlockchainSdkError()
    }

    companion object {
        // https://github.com/casper-network/casper-node/blob/dev/node/src/components/rpc_server/rpcs/error_code.rs
        private const val ERROR_CODE_QUERY_FAILED = -32003
    }
}