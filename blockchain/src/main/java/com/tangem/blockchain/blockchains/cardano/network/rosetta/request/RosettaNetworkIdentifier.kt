package com.tangem.blockchain.blockchains.cardano.network.rosetta.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class RosettaNetworkIdentifier(
    @Json(name = "blockchain") val blockchain: String,
    @Json(name = "network") val network: String,
)