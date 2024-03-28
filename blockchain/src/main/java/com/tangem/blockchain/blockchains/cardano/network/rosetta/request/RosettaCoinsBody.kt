package com.tangem.blockchain.blockchains.cardano.network.rosetta.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class RosettaCoinsBody(
    @Json(name = "network_identifier") val networkIdentifier: RosettaNetworkIdentifier,
    @Json(name = "account_identifier") val accountIdentifier: AccountIdentifier,
) {

    @JsonClass(generateAdapter = true)
    data class AccountIdentifier(
        @Json(name = "address") val address: String,
    )
}