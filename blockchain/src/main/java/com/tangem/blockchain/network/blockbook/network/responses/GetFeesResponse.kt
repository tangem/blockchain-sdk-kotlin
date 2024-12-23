package com.tangem.blockchain.network.blockbook.network.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetFeesResponse(
    @Json(name = "result") val result: Double,
)