package com.tangem.blockchain.blockchains.kaspa.krc20.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class Envelope(
    @Json(name = "p") val p: String,
    @Json(name = "op") val op: String,
    @Json(name = "amt") val amt: String,
    @Json(name = "to") val to: String,
    @Json(name = "tick") val tick: String,
)