package com.tangem.blockchain.blockchains.near.network

import com.tangem.blockchain.common.JsonRPCRequest
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * NearApi provides access to posting requests to jsonRPC endpoints.
 * @see <a href="https://docs.near.org/api/rpc/introduction">NEAR RPC API</a>
 *
[REDACTED_AUTHOR]
 */
interface NearApi {

    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST("jsonRPC")
    suspend fun post(@Body body: JsonRPCRequest): ResponseBody
}

sealed interface NearMethod {

    fun asRequestBody(): JsonRPCRequest

    sealed class Account : NearMethod {

        data class View(val accountId: String) : Account() {
            override fun asRequestBody(): JsonRPCRequest = JsonRPCRequest(
                method = "query",
                params = mapOf(
                    "request_type" to "view_account",
                    "finality" to "final",
                    "account_id" to accountId,
                ),
            )
        }
    }

    sealed class GasPrice : NearMethod {

        data class BlockHeight(val blockHeight: Long) : GasPrice() {
            override fun asRequestBody(): JsonRPCRequest = JsonRPCRequest(
                method = "gas_price",
                params = arrayOf(blockHeight),
            )
        }

        data class BlockHash(val blockHash: String) : GasPrice() {
            override fun asRequestBody(): JsonRPCRequest = JsonRPCRequest(
                method = "gas_price",
                params = arrayOf(blockHash),
            )
        }

        object Null : GasPrice() {
            override fun asRequestBody(): JsonRPCRequest = JsonRPCRequest(
                method = "gas_price",
                params = arrayOfNulls<Any>(1),
            )
        }
    }

    sealed class Transaction : NearMethod {

        data class SendTxAsync(val signedTxBase64: String) : Transaction() {
            override fun asRequestBody(): JsonRPCRequest = JsonRPCRequest(
                method = "broadcast_tx_async",
                params = arrayOf(signedTxBase64),
            )
        }

        data class Status(val txHash: String, val senderAccountId: String) : Transaction() {
            override fun asRequestBody(): JsonRPCRequest = JsonRPCRequest(
                method = "tx",
                params = arrayOf(txHash, senderAccountId),
            )
        }
    }
}