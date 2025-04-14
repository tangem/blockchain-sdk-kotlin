package com.tangem.blockchain.blockchains.cardano.network.adalite.repsonse

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class AdaliteAddressResponse(
    @Json(name = "Right") val successData: SuccessData,
) {

    @JsonClass(generateAdapter = true)
    data class SuccessData(
        @Json(name = "caTxList") val transactions: List<Transaction>,
    ) {

        @JsonClass(generateAdapter = true)
        data class Transaction(
            @Json(name = "ctbId") val hash: String,
        )
    }
}