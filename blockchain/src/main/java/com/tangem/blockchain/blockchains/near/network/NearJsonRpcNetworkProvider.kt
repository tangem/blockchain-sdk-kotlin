package com.tangem.blockchain.blockchains.near.network

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.moshi

/**
[REDACTED_AUTHOR]
 */
class NearJsonRpcNetworkProvider(
    override val host: String,
    private val api: NearApi,
) : NearNetworkProvider {

    private val accountAdapter = moshi.adapter<NearResponse<ViewAccountResult>>(
        Types.newParameterizedType(
            NearResponse::class.java,
            ViewAccountResult::class.java,
        )
    )

    private val gasAdapter = moshi.adapter<NearResponse<GasPriceResult>>(
        Types.newParameterizedType(
            NearResponse::class.java,
            GasPriceResult::class.java,
        )
    )

    private val sendAdapter = moshi.adapter<NearResponse<SendTransactionAsyncResult>>(
        Types.newParameterizedType(
            NearResponse::class.java,
            SendTransactionAsyncResult::class.java,
        )
    )

    private val txStatusAdapter = moshi.adapter<NearResponse<TransactionStatusResult>>(
        Types.newParameterizedType(
            NearResponse::class.java,
            SendTransactionAsyncResult::class.java,
        )
    )

    override suspend fun getAccount(address: String): Result<ViewAccountResult> {
        return try {
            postMethod(NearMethod.Account.View(address), accountAdapter).toResult()
        } catch (ex: Exception) {
            Result.Failure(ex.toBlockchainSdkError())
        }
    }

    override suspend fun getGas(blockHeight: Long): Result<GasPriceResult> {
        return try {
            postMethod(NearMethod.GasPrice.BlockHeight(blockHeight), gasAdapter).toResult()
        } catch (ex: Exception) {
            Result.Failure(ex.toBlockchainSdkError())
        }
    }

    override suspend fun getTransactionStatus(
        txHash: String,
        senderAccountId: String,
    ): Result<TransactionStatusResult> {
        return try {
            postMethod(NearMethod.Transaction.Status(txHash, senderAccountId), txStatusAdapter).toResult()
        } catch (ex: Exception) {
            Result.Failure(ex.toBlockchainSdkError())
        }
    }

    override suspend fun sendTransaction(signedTxBase64: String): Result<SendTransactionAsyncResult> {
        return try {
            postMethod(NearMethod.Transaction.SendTxAsync(signedTxBase64), sendAdapter).toResult()
        } catch (ex: Exception) {
            Result.Failure(ex.toBlockchainSdkError())
        }
    }

    @Throws(IllegalArgumentException::class)
    private suspend fun <T> postMethod(method: NearMethod, adapter: JsonAdapter<T>): T {
        val responseBody = api.post(method.asRequestBody())
        return requireNotNull(
            value = adapter.fromJson(responseBody.string()) as T,
            lazyMessage = { "Can not parse response" },
        )
    }

    private fun <T> NearResponse<T>.toResult(): Result<T> {
        return when {
            result != null && error == null -> Result.Success(result)
            result == null && error != null -> Result.Failure(BlockchainSdkError.Near.Api(error.code, error.message))
            else -> Result.Failure(
                BlockchainSdkError.UnsupportedOperation(
                    "Instance of the NearResponse is broken. Result and Error can't be null. Based on JSONRPC 2.0"
                )
            )
        }
    }
}
