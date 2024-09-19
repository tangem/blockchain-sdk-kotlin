package com.tangem.blockchain.blockchains.sui.network.rpc

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import com.tangem.blockchain.blockchains.sui.network.SuiConstants
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.blockchain.network.moshi
import retrofit2.create
import java.math.BigDecimal

internal class SuiJsonRpcProvider(override val baseUrl: String) : NetworkProvider {

    private val api: SuiApi by lazy {
        createRetrofitInstance(baseUrl).create()
    }

    suspend fun getReferenceGasPrice(): Result<BigDecimal> = rpcCall(
        method = Method.GetReferenceGasPrice,
    )
    suspend fun getCoins(address: String, coinType: String = SuiConstants.COIN_TYPE): Result<SuiCoinsResponse> =
        rpcCall(
            method = Method.GetCoins(address, coinType),
        )

    suspend fun dryRunTransaction(transactionHash: String): Result<SuiDryRunTransactionResponse> = rpcCall(
        method = Method.DryRunTransactionBlock(transactionHash),
    )

    suspend fun executeTransaction(
        transactionHash: String,
        signature: String,
    ): Result<SuiExecuteTransactionBlockResponse> = rpcCall(
        method = Method.ExecuteTransactionBlock(transactionHash, signature),
    )

    suspend fun getTransactionBlock(transactionHash: String): Result<SuiGetTransactionBlockResponse> = rpcCall(
        method = Method.GetTransactionBlock(transactionHash),
    )

    private suspend inline fun <reified T : Any> rpcCall(method: Method<T>): Result<T> = try {
        val body = SuiJsonRpcRequestBody(
            method = method.method,
            params = method.params,
        )
        val responseBody = api.post(body = body)
        val response = method.adapter.fromJson(responseBody.string())

        unwrapResponse(response)
    } catch (e: Exception) {
        Result.Failure(e.toBlockchainSdkError())
    }

    private inline fun <reified T : Any> unwrapResponse(response: SuiJsonRpcResponse<T>?): Result<T> = when {
        response?.error != null -> Result.Failure(
            error = BlockchainSdkError.Sui.Api(
                code = response.error.code,
                message = response.error.message,
            ),
        )
        response?.result != null -> Result.Success(response.result)
        else -> Result.Failure(
            error = BlockchainSdkError.WrappedThrowable(
                throwable = IllegalStateException("Unknown response"),
            ),
        )
    }

    sealed class Method<T : Any>(val method: String) {

        abstract val adapter: JsonAdapter<SuiJsonRpcResponse<T>>

        open val params: List<Any> = emptyList()

        data object GetReferenceGasPrice : Method<BigDecimal>(
            method = "suix_getReferenceGasPrice",
        ) {

            override val adapter: JsonAdapter<SuiJsonRpcResponse<BigDecimal>> = moshi.adapter(
                Types.newParameterizedType(
                    SuiJsonRpcResponse::class.java,
                    BigDecimal::class.java,
                ),
            )
        }

        class GetCoins(address: String, coinType: String) : Method<SuiCoinsResponse>(
            method = "suix_getCoins",
        ) {

            override val adapter: JsonAdapter<SuiJsonRpcResponse<SuiCoinsResponse>> = moshi.adapter(
                Types.newParameterizedType(
                    SuiJsonRpcResponse::class.java,
                    SuiCoinsResponse::class.java,
                ),
            )

            override val params: List<String> = listOf(address, coinType)
        }

        class DryRunTransactionBlock(transactionHash: String) : Method<SuiDryRunTransactionResponse>(
            method = "sui_dryRunTransactionBlock",
        ) {

            override val adapter: JsonAdapter<SuiJsonRpcResponse<SuiDryRunTransactionResponse>> = moshi.adapter(
                Types.newParameterizedType(
                    SuiJsonRpcResponse::class.java,
                    SuiDryRunTransactionResponse::class.java,
                ),
            )

            override val params: List<String> = listOf(transactionHash)
        }

        class ExecuteTransactionBlock(
            transactionHash: String,
            signature: String,
        ) : Method<SuiExecuteTransactionBlockResponse>(method = "sui_executeTransactionBlock") {

            override val adapter: JsonAdapter<SuiJsonRpcResponse<SuiExecuteTransactionBlockResponse>> = moshi.adapter(
                Types.newParameterizedType(
                    SuiJsonRpcResponse::class.java,
                    SuiExecuteTransactionBlockResponse::class.java,
                ),
            )

            override val params: List<Any> = listOf(
                transactionHash,
                listOf(signature),
                TransactionBlockResponseOptions(),
            )
        }

        class GetTransactionBlock(transactionHash: String) : Method<SuiGetTransactionBlockResponse>(
            method = "sui_getTransactionBlock",
        ) {
            override val adapter: JsonAdapter<SuiJsonRpcResponse<SuiGetTransactionBlockResponse>> = moshi.adapter(
                Types.newParameterizedType(
                    SuiJsonRpcResponse::class.java,
                    SuiGetTransactionBlockResponse::class.java,
                ),
            )

            override val params: List<Any> = listOf(
                transactionHash,
                TransactionBlockResponseOptions(),
            )
        }

        private class TransactionBlockResponseOptions private constructor(
            map: Map<String, Boolean>,
        ) : Map<String, Boolean> by map {

            constructor(
                showInput: Boolean = false,
                showRawInput: Boolean = false,
                showEffects: Boolean = false,
                showEvents: Boolean = false,
                showObjectChanges: Boolean = false,
                showBalanceChanges: Boolean = false,
                showRawEffects: Boolean = false,
            ) : this(
                map = mapOf(
                    "showInput" to showInput,
                    "showRawInput" to showRawInput,
                    "showEffects" to showEffects,
                    "showEvents" to showEvents,
                    "showObjectChanges" to showObjectChanges,
                    "showBalanceChanges" to showBalanceChanges,
                    "showRawEffects" to showRawEffects,
                ),
            )
        }
    }
}