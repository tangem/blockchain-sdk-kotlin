package com.tangem.blockchain.network.blockbook.network.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetFeeResponse(
    @Json(name = "id") val id: String,
    @Json(name = "result") val result: Result,
) {

    @JsonClass(generateAdapter = true)
    data class Result(
        @Json(name = "blocks") val blocks: Int,
        @Json(name = "feerate") val feerate: Double,
    )
}
