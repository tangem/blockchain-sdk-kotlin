package com.tangem.blockchain.network.blockbook.network.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetUtxoResponseItem(
    @Json(name = "confirmations") val confirmations: Int,
    @Json(name = "height") val height: Int?,
    @Json(name = "txid") val txid: String,
    @Json(name = "value") val value: String,
    @Json(name = "vout") val vout: Int,
)
