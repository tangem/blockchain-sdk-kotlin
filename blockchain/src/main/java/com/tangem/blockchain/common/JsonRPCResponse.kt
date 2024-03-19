package com.tangem.blockchain.common

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * JsonRPC response object
 * @see <a href="https://www.jsonrpc.org/specification">Specification</a>
 */
@JsonClass(generateAdapter = true)
internal data class JsonRPCResponse(
    @Json(name = "id") val id: String,
    @Json(name = "jsonrpc") val jsonRpc: String,
    @Json(name = "result") val result: Any?,
    @Json(name = "error") val error: Error?,
) {
    @JsonClass(generateAdapter = true)
    data class Error(
        @Json(name = "code") val code: Int,
        @Json(name = "message") val message: String,
    )
}
