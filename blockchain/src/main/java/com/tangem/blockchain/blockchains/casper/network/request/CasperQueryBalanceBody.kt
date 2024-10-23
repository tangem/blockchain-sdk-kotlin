package com.tangem.blockchain.blockchains.casper.network.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class CasperQueryBalanceBody(
    @Json(name = "purse_identifier") val purseIdentifier: PurseIdentifier,
) {
    @JsonClass(generateAdapter = true)
    data class PurseIdentifier(
        @Json(name = "main_purse_under_public_key") val mainPurseUnderPublicKey: String,
    )
}