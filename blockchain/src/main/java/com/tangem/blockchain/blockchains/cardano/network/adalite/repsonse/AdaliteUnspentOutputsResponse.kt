package com.tangem.blockchain.blockchains.cardano.network.adalite.repsonse

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class AdaliteUnspentOutputsResponse(
    @Json(name = "Right") val successData: List<Utxo>,
) {

    @JsonClass(generateAdapter = true)
    data class Utxo(
        @Json(name = "cuAddress") val address: String,
        @Json(name = "cuId") val hash: String,
        @Json(name = "cuOutIndex") val outputIndex: Int,
        @Json(name = "cuCoins") val amountData: AdaliteBalance,
    )
}