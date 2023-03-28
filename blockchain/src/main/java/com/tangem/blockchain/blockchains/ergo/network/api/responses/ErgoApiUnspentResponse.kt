package com.tangem.blockchain.blockchains.ergo.network.api.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ErgoApiUnspentResponse(
    @Json(name = "id")
    var id: String? = null,
    @Json(name = "txId")
    var txId: String? = null,
    @Json(name = "value")
    var value: Long? = null,
    @Json(name = "index")
    var index: Int? = null,
    @Json(name = "creationHeight")
    var creationHeight: Int? = null,
    @Json(name = "ergoTree")
    var ergoTree: String? = null,
    @Json(name = "address")
    var address: String? = null,
    @Json(name = "assets")
    var assets: List<Assets>? = null,
    @Json(name = "spentTransactionId")
    var spentTransactionId: String? = null,
    @Json(name = "mainChain")
    var mainChain: Boolean? = null,
    @Json(name = "additionalRegisters")
    var additionalRegisters: Map<String, String>? = emptyMap(),
)

@JsonClass(generateAdapter = true)
data class Assets(
    @Json(name = "tokenId")
    var tokenId: String? = null,
    @Json(name = "index")
    var index: Int? = null,
    @Json(name = "amount")
    var amount: Int? = null,
    @Json(name = "name")
    var name: String? = null,
    @Json(name = "decimals")
    var decimals: Int? = null,
    @Json(name = "type")
    var type: String? = null,
)
