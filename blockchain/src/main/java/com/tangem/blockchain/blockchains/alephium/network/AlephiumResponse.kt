package com.tangem.blockchain.blockchains.alephium.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

object AlephiumResponse {
    @JsonClass(generateAdapter = true)
    data class Utxos(
        @Json(name = "utxos") val utxos: List<Utxo>,
    )

    @JsonClass(generateAdapter = true)
    data class Utxo(
        @Json(name = "additionalData") val additionalData: String?,
        @Json(name = "amount") val amount: String,
        @Json(name = "lockTime") val lockTime: Long = 0,
        @Json(name = "ref") val ref: Ref,
        @Json(name = "tokens") val tokens: List<Token>?,
    ) {
        val isConfirmed get() = lockTime != 0L
        fun isNotFromFuture(nowMillis: Long) = lockTime <= nowMillis

        @JsonClass(generateAdapter = true)
        data class Ref(
            @Json(name = "hint") val hint: Int,
            @Json(name = "key") val key: String,
        )

        @JsonClass(generateAdapter = true)
        data class Token(
            @Json(name = "amount") val amount: String,
            @Json(name = "id") val id: String,
        )
    }

    @JsonClass(generateAdapter = true)
    data class UnsignedTx(
        @Json(name = "fromGroup") val fromGroup: Int,
        @Json(name = "gasAmount") val gasAmount: String,
        @Json(name = "gasPrice") val gasPrice: String,
        @Json(name = "toGroup") val toGroup: Int,
        @Json(name = "txId") val txId: String,
        @Json(name = "unsignedTx") val unsignedTx: String,
    )

    data class SubmitTx(
        @Json(name = "txId")
        val txId: String,
        @Json(name = "fromGroup")
        val fromGroup: Int,
        @Json(name = "toGroup")
        val toGroup: Int,
    )
}