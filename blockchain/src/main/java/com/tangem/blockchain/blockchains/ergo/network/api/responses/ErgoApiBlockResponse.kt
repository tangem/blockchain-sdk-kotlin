package com.tangem.blockchain.blockchains.ergo.network.api.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ErgoApiBlockResponse(
    @Json(name = "items")
    var items: List<Items>? = null,
    @Json(name = "total")
    var total: Int? = null,
)

@JsonClass(generateAdapter = true)
data class Items(
    @Json(name = "height")
    var height: Long? = null,
)
