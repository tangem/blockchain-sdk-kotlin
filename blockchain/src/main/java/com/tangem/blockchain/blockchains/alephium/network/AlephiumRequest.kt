package com.tangem.blockchain.blockchains.alephium.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

object AlephiumRequest {

    @JsonClass(generateAdapter = true)
    data class BuildTx(
        @Json(name = "destinations") val destinations: List<Destination>,
        @Json(name = "fromPublicKey") val fromPublicKey: String,
        @Json(name = "gasAmount") val gasAmount: String? = null,
        @Json(name = "gasPrice") val gasPrice: String? = null,
    ) {
        @JsonClass(generateAdapter = true)
        data class Destination(
            @Json(name = "address") val address: String,
            @Json(name = "attoAlphAmount") val attoAlphAmount: String,
        )
    }

    @JsonClass(generateAdapter = true)
    data class DecodeTx(
        @Json(name = "unsignedTx") val unsignedTx: String,
    )

    @JsonClass(generateAdapter = true)
    data class SubmitTx(
        @Json(name = "unsignedTx")
        val unsignedTx: String,
        @Json(name = "signature")
        val signature: String,
    )
}