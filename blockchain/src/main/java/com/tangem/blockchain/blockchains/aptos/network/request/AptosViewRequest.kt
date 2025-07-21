package com.tangem.blockchain.blockchains.aptos.network.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class AptosViewRequest(
    @Json(name = "function") val function: String,
    @Json(name = "type_arguments") val typeArguments: List<String>,
    @Json(name = "arguments") val arguments: List<String>,
)