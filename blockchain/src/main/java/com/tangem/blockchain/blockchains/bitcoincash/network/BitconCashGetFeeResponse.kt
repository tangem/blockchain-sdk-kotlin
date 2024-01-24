package com.tangem.blockchain.blockchains.bitcoincash.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BitconCashGetFeeResponse(
    @Json(name = "id") val id: String,
    @Json(name = "result") val result: Double,
)