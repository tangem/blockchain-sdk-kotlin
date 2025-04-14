package com.tangem.blockchain.blockchains.ducatus.network.bitcore

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BitcoreBalance(
    @Json(name = "confirmed")
    val confirmed: Long? = null,

    @Json(name = "unconfirmed")
    val unconfirmed: Long? = null,
)

@JsonClass(generateAdapter = true)
data class BitcoreUtxo(
    @Json(name = "mintTxid")
    val transactionHash: String? = null,

    @Json(name = "mintIndex")
    val index: Int? = null,

    @Json(name = "value")
    val amount: Long? = null,

    @Json(name = "script")
    val script: String? = null,
)

@JsonClass(generateAdapter = true)
data class BitcoreSendResponse(
    @Json(name = "txid")
    val txid: String? = null,
)