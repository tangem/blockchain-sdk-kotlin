package com.tangem.blockchain.blockchains.cardano.network.adalite

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AdaliteAddress(
        @Json(name = "Right")
        var data: AdaliteAddressData? = null
)

@JsonClass(generateAdapter = true)
data class AdaliteAddressData(
        @Json(name = "caBalance")
        var balanceData: AdaliteBalance? = null,

        @Json(name = "caTxList")
        var transactions: List<AdaliteTransaction>?
)

@JsonClass(generateAdapter = true)
data class AdaliteBalance(
        @Json(name = "getCoin")
        var amount: Long? = null
)

@JsonClass(generateAdapter = true)
data class AdaliteUnspents(
        @Json(name = "Right")
        var data: List<AdaliteUtxo>?
)

@JsonClass(generateAdapter = true)
data class AdaliteUtxo(
        @Json(name = "cuAddress")
        var address: String? = null,

        @Json(name = "cuId")
        var hash: String? = null,

        @Json(name = "cuOutIndex")
        var outputIndex: Int? = null,

        @Json(name = "cuCoins")
        var amountData: AdaliteBalance? = null
)

@JsonClass(generateAdapter = true)
data class AdaliteTransaction(
        @Json(name = "ctbId")
        var hash: String? = null
)