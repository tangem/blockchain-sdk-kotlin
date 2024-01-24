package com.tangem.blockchain.blockchains.bitcoincash.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal class SendTransactionRequest(
    @Json(name = "jsonrpc") val jsonrpc: String,
    @Json(name = "id") val id: String,
    @Json(name = "method") val method: String,
    @Json(name = "params") val params: List<String>,
) {
    companion object {

        fun getSendRequest(signedTransaction: String): SendTransactionRequest {
            return SendTransactionRequest(
                jsonrpc = "2.0",
                id = "id",
                method = "sendrawtransaction",
                params = listOf(signedTransaction),
            )
        }
    }
}