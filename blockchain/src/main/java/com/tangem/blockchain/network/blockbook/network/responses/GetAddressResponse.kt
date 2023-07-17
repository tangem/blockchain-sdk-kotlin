package com.tangem.blockchain.network.blockbook.network.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetAddressResponse(
    @Json(name = "balance") val balance: String,
    @Json(name = "unconfirmedTxs") val unconfirmedTxs: Int,
    @Json(name = "txs") val txs: Int,
    @Json(name = "transactions") val transactions: List<Transaction>?,
) {

    @JsonClass(generateAdapter = true)
    data class Transaction(
        @Json(name = "txid") val txid: String,
        @Json(name = "vout") val vout: List<Vout>?,
        @Json(name = "confirmations") val confirmations: Int,
        @Json(name = "blockTime") val blockTime: Int,
        @Json(name = "value") val value: String,
        @Json(name = "vin") val vin: List<Vin>?,
        @Json(name = "fees") val fees: String,
    ) {

        @JsonClass(generateAdapter = true)
        data class Vin(
            @Json(name = "addresses") val addresses: List<String>,
            @Json(name = "value") val value: String,
        )

        @JsonClass(generateAdapter = true)
        data class Vout(
            @Json(name = "addresses") val addresses: List<String>,
            @Json(name = "hex") val hex: String,
            @Json(name = "value") val value: String,
        )
    }
}
