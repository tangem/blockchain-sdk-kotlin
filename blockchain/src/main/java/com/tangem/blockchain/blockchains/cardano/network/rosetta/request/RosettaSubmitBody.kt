package com.tangem.blockchain.blockchains.cardano.network.rosetta.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class RosettaSubmitBody(
    @Json(name = "network_identifier") val networkIdentifier: RosettaNetworkIdentifier,
    @Json(name = "signed_transaction") val signedTransaction: String,
)