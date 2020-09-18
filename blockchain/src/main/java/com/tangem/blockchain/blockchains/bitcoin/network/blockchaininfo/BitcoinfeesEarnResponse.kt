package com.tangem.blockchain.blockchains.bitcoin.network.blockchaininfo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BitcoinfeesEarnResponse(
    @Json(name = "hourFee")
    val minimalFeePerByte: Int? = null,

    @Json(name = "halfHourFee")
    val normalFeePerByte: Int? = null,

    @Json(name = "fastestFee")
    val priorityFeePerByte: Int? = null
)