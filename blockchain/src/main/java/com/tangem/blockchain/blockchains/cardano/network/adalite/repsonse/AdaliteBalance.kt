package com.tangem.blockchain.blockchains.cardano.network.adalite.repsonse

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class AdaliteBalance(
    @Json(name = "getCoin") val amount: Long,
    @Json(name = "getTokens") val tokens: List<Token>,
) {

    @JsonClass(generateAdapter = true)
    data class Token(
        @Json(name = "assetName") val assetName: String,
        @Json(name = "quantity") val quantity: Long,
        @Json(name = "policyId") val policyId: String,
    )
}