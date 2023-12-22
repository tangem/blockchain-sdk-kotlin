package com.tangem.blockchain.common

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.UUID

/**
 * @author Anton Zhilenkov on 31.07.2023.
 */
@JsonClass(generateAdapter = true)
data class JsonRPCRequest(
    @Json(name = "method") val method: String,
    @Json(name = "params") val params: Any,
    @Json(name = "id") val id: String = UUID.randomUUID().toString(),
    @Json(name = "jsonrpc") val jsonRpc: String = "2.0",
)
