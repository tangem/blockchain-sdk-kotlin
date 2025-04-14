package com.tangem.blockchain.common

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.UUID

/**
 * JsonRPC request object
 *
 * @see <a href="https://www.jsonrpc.org/specification">Specification</a>
[REDACTED_AUTHOR]
 */
@JsonClass(generateAdapter = true)
internal data class JsonRPCRequest(
    @Json(name = "method") val method: String,
    @Json(name = "params") val params: Any?,
    @Json(name = "id") val id: String = UUID.randomUUID().toString(),
    @Json(name = "jsonrpc") val jsonRpc: String = "2.0",
)