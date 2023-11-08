package com.tangem.blockchain.blockchains.near.network

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import com.tangem.blockchain.blockchains.near.network.api.*
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.moshi

/**
 * @author Anton Zhilenkov on 01.08.2023.
 */
class NearJsonRpcNetworkProvider(
    override val baseUrl: String,
    private val api: NearApi,
    private val urlPostfix: String = "",
) : NearNetworkProvider {

    private val protocolConfigAdapter = moshi.adapter<NearResponse<ProtocolConfigResult>>(
        Types.newParameterizedType(
            NearResponse::class.java,
            ProtocolConfigResult::class.java,
        )
    )

    private val networkStatusAdapter = moshi.adapter<NearResponse<NetworkStatusResult>>(
        Types.newParameterizedType(
            NearResponse::class.java,
            NetworkStatusResult::class.java,
        )
    )

    private val accessKeyResultAdapter = moshi.adapter<NearResponse<AccessKeyResult>>(
        Types.newParameterizedType(
            NearResponse::class.java,
            AccessKeyResult::class.java,
        )
    )

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
            TransactionStatusResult::class.java,
        )
    )

    override suspend fun getProtocolConfig(): Result<ProtocolConfigResult> {
        return try {
            postMethod(NearMethod.ProtocolConfig, protocolConfigAdapter).toResult()
        } catch (ex: Exception) {
            Result.Failure(ex.toBlockchainSdkError())
        }
    }

    override suspend fun getNetworkStatus(): Result<NetworkStatusResult> {
        return try {
            postMethod(NearMethod.NetworkStatus, networkStatusAdapter).toResult()
        } catch (ex: Exception) {
            Result.Failure(ex.toBlockchainSdkError())
        }
    }

    override suspend fun getAccessKey(params: NearGetAccessKeyParams): Result<AccessKeyResult> {
        return try {
            postMethod(NearMethod.AccessKey.View(params.address, params.publicKeyEncodedToBase58), accessKeyResultAdapter).toResult()
        } catch (ex: Exception) {
            Result.Failure(ex.toBlockchainSdkError())
        }
    }

    override suspend fun getAccount(address: String): Result<ViewAccountResult> {
        return try {
            postMethod(NearMethod.Account.View(address), accountAdapter).toResult()
        } catch (ex: Exception) {
            Result.Failure(ex.toBlockchainSdkError())
        }
    }

    override suspend fun getGas(blockHash: String): Result<GasPriceResult> {
        return try {
            postMethod(NearMethod.GasPrice.BlockHash(blockHash), gasAdapter).toResult()
        } catch (ex: Exception) {
            Result.Failure(ex.toBlockchainSdkError())
        }
    }

    override suspend fun getTransactionStatus(params: NearGetTxParams): Result<TransactionStatusResult> {
        return try {
            postMethod(NearMethod.Transaction.Status(params.txHash, params.senderId), txStatusAdapter).toResult()
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
        val responseBody = api.sendJsonRpc(method.asRequestBody(), urlPostfix)
        return requireNotNull(
            value = adapter.fromJson(responseBody.string()) as T,
            lazyMessage = { "Can not parse response" },
        )
    }

    private fun <T> NearResponse<T>.toResult(): Result<T> {
        return when {
            result != null && error == null -> Result.Success(result)
            result == null && error != null -> Result.Failure(
                BlockchainSdkError.NearException.Api(
                    name = error.cause.name,
                    code = error.cause.name.hashCode(),
                    message = error.cause.info.toString(),
                )
            )

            else -> Result.Failure(
                BlockchainSdkError.UnsupportedOperation(
                    "Instance of the NearResponse is broken. Result and Error can't be null. Based on JSONRPC 2.0"
                )
            )
        }
    }
}

