package com.tangem.blockchain.blockchains.filecoin.network.provider

import com.squareup.moshi.adapter
import com.tangem.blockchain.blockchains.filecoin.models.FilecoinAccountInfo
import com.tangem.blockchain.blockchains.filecoin.models.FilecoinTxGasInfo
import com.tangem.blockchain.blockchains.filecoin.models.FilecoinTxInfo
import com.tangem.blockchain.blockchains.filecoin.network.FilecoinApi
import com.tangem.blockchain.blockchains.filecoin.network.FilecoinNetworkProvider
import com.tangem.blockchain.blockchains.filecoin.network.converters.FilecoinTxGasInfoConverter
import com.tangem.blockchain.blockchains.filecoin.network.request.FilecoinRpcBody
import com.tangem.blockchain.blockchains.filecoin.network.request.FilecoinRpcBodyFactory
import com.tangem.blockchain.blockchains.filecoin.network.request.FilecoinSignedTransactionBody
import com.tangem.blockchain.blockchains.filecoin.network.response.FilecoinRpcResponse
import com.tangem.blockchain.blockchains.filecoin.network.response.FilecoinRpcResponseResult
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.blockchain.network.moshi
import kotlinx.io.IOException
import okhttp3.Interceptor
import java.math.BigDecimal

/**
 * Filecoin rpc network provider
 *
 * @property baseUrl         provider base url
 * @property postfixUrl      postfix url. Hack for supports base url without '/'
 * @param headerInterceptors header interceptors
 */
@OptIn(ExperimentalStdlibApi::class)
internal class FilecoinRpcNetworkProvider(
    override val baseUrl: String,
    private val postfixUrl: String,
    headerInterceptors: List<Interceptor> = emptyList(),
) : FilecoinNetworkProvider {

    private val api = createRetrofitInstance(baseUrl, headerInterceptors).create(FilecoinApi::class.java)

    override suspend fun getAccountInfo(address: String): Result<FilecoinAccountInfo> {
        return post(
            body = FilecoinRpcBodyFactory.createGetActorInfoBody(address),
            onSuccess = { response: FilecoinRpcResponseResult.GetActorInfo ->
                FilecoinAccountInfo(balance = BigDecimal(response.balance), nonce = response.nonce)
            },
            onFailure = { response ->
                if (response.isNoActorError()) BlockchainSdkError.AccountNotFound() else toDefaultError(response)
            },
        )
    }

    override suspend fun estimateMessageGas(transactionInfo: FilecoinTxInfo): Result<FilecoinTxGasInfo> {
        return post(
            body = FilecoinRpcBodyFactory.createGetMessageGasBody(transactionInfo),
            onSuccess = FilecoinTxGasInfoConverter::convert,
        )
    }

    override suspend fun submitTransaction(signedTransactionBody: FilecoinSignedTransactionBody): Result<String> {
        return post(
            body = FilecoinRpcBodyFactory.createSubmitTransactionBody(signedTransactionBody),
            onSuccess = FilecoinRpcResponseResult.SubmitTransaction::hash,
        )
    }

    private fun FilecoinRpcResponse.Failure.isNoActorError() = message.endsWith("actor not found")

    private suspend inline fun <reified Data, Domain> post(
        body: FilecoinRpcBody,
        onSuccess: (Data) -> Domain,
        onFailure: (FilecoinRpcResponse.Failure) -> BlockchainSdkError = ::toDefaultError,
    ): Result<Domain> {
        return try {
            when (val response = api.post(body = body, postfixUrl = postfixUrl)) {
                is FilecoinRpcResponse.Success -> {
                    Result.Success(
                        data = onSuccess(
                            moshi.adapter<Data>().fromJsonValue(response.result)!!,
                        ),
                    )
                }
                is FilecoinRpcResponse.Failure -> Result.Failure(error = onFailure(response))
            }
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private fun toDefaultError(response: FilecoinRpcResponse.Failure): BlockchainSdkError {
        return IOException(response.message).toBlockchainSdkError()
    }
}