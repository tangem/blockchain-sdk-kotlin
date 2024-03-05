package com.tangem.blockchain.common

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JsonRPCResponse(
    @Json(name = "id") val id: String,
    @Json(name = "jsonrpc") val jsonRpc: String,
    @Json(name = "result") val result: Any?,
)