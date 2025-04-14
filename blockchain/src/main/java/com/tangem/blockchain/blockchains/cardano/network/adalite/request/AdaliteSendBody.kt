package com.tangem.blockchain.blockchains.cardano.network.adalite.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class AdaliteSendBody(
    @Json(name = "signedTx") val signedTransaction: String,
)