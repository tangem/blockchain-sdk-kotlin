package com.tangem.blockchain.blockchains.ergo.network.api.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ErgoApiAddress(
    @Json(name = "nanoErgs")
    var balance: Long? = null,
    @Json(name = "tokens")
    var tokens: List<Token>?,
)

@JsonClass(generateAdapter = true)
data class Token(
    @Json(name = "tokenId")
    var tokenId: String? = null,
    @Json(name = "amount")
    var amount: Int? = null,
    @Json(name = "decimals")
    var decimals: Int? = null,
    @Json(name = "name")
    var name: String? = null,
    @Json(name = "tokenType")
    var tokenType: String? = null,
)




