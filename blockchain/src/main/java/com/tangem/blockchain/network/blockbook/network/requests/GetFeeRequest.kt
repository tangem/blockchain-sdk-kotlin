package com.tangem.blockchain.network.blockbook.network.requests

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal class GetFeeRequest(
    @Json(name = "jsonrpc") val jsonrpc: String,
    @Json(name = "id") val id: String,
    @Json(name = "method") val method: String,
    @Json(name = "params") val params: List<Int>,
) {
    companion object {

        fun getFee(param: Int): GetFeeRequest {
            return GetFeeRequest(
                jsonrpc = "2.0",
                id = "id",
                method = "estimatesmartfee",
                params = listOf(param),
            )
        }
    }
}
