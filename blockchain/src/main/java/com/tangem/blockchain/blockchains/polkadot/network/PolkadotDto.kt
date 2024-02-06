package com.tangem.blockchain.blockchains.polkadot.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PolkadotBody(
    val method: String,
    val params: List<Any> = listOf(),
    val jsonrpc: String = "2.0",
    val id: Int = 4,
)

@JsonClass(generateAdapter = true)
data class PolkadotResponse(
    @Json(name = "jsonrpc")
    val jsonrpc: String = "",

    @Json(name = "id")
    val id: Int? = null,

    @Json(name = "result")
    val result: Map<String, Any>? = null,

    @Json(name = "error")
    val error: PolkadotError? = null,
)

@JsonClass(generateAdapter = true)
data class PolkadotError(
    @Json(name = "code")
    val code: Int? = null,

    @Json(name = "message")
    val message: String? = null,
)

enum class PolkadotMethod(val method: String) {
    GET_FEE("payment_queryInfo"),
}
