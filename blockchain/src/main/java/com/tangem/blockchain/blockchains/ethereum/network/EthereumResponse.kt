package com.tangem.blockchain.blockchains.ethereum.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// TODO(https://tangem.atlassian.net/browse/AND-8313): Use JsonRPCResponse
@JsonClass(generateAdapter = true)
data class EthereumResponse(
    @Json(name = "jsonrpc")
    val jsonrpc: String = "",

    @Json(name = "id")
    val id: Int? = null,

    @Json(name = "result")
    val result: Any? = null,

    @Json(name = "error")
    val error: EthereumError? = null,
)

@JsonClass(generateAdapter = true)
data class EthereumError(
    @Json(name = "code")
    val code: Int? = null,

    @Json(name = "message")
    val message: String? = null,
)
