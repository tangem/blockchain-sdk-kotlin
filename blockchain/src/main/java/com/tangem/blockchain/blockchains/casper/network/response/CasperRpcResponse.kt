package com.tangem.blockchain.blockchains.casper.network.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Casper RPC response */
internal sealed interface CasperRpcResponse {

    /** Success response with [result] */
    @JsonClass(generateAdapter = true)
    data class Success(
        @Json(name = "result")
        val result: Any,
    ) : CasperRpcResponse

    /** Failure response with error [message] */
    @JsonClass(generateAdapter = true)
    data class Failure(
        @Json(name = "message") val message: String,
        @Json(name = "code") val code: Int = 0,
    ) : CasperRpcResponse
}