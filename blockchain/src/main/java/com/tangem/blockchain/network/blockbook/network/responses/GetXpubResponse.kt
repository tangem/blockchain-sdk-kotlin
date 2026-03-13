package com.tangem.blockchain.network.blockbook.network.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetXpubResponse(
    @Json(name = "page")
    val page: Int?,
    @Json(name = "totalPages")
    val totalPages: Int?,
    @Json(name = "itemsOnPage")
    val itemsOnPage: Int?,
    @Json(name = "address")
    val address: String?,
    @Json(name = "balance")
    val balance: String,
    @Json(name = "totalReceived")
    val totalReceived: String?,
    @Json(name = "totalSent")
    val totalSent: String?,
    @Json(name = "unconfirmedBalance")
    val unconfirmedBalance: String?,
    @Json(name = "unconfirmedTxs")
    val unconfirmedTxs: Int?,
    @Json(name = "txs")
    val txs: Int,
    @Json(name = "usedTokens")
    val usedTokens: Int?,
    @Json(name = "transactions")
    val transactions: List<GetAddressResponse.Transaction>?,
    @Json(name = "tokens")
    val tokens: List<XpubToken>?,
) {

    @JsonClass(generateAdapter = true)
    data class XpubToken(
        @Json(name = "type")
        val type: String?,
        @Json(name = "name")
        val name: String,
        @Json(name = "path")
        val path: String,
        @Json(name = "transfers")
        val transfers: Int?,
        @Json(name = "decimals")
        val decimals: Int?,
        @Json(name = "balance")
        val balance: String?,
        @Json(name = "totalReceived")
        val totalReceived: String?,
        @Json(name = "totalSent")
        val totalSent: String?,
    )
}