package com.tangem.blockchain.transactionhistory.blockchains.algorand.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class AlgorandTransactionHistoryResponse(
    @Json(name = "next-token") val nextToken: String?,
    @Json(name = "transactions") val transactions: List<AlgorandTransactionHistoryItem>,
)

@JsonClass(generateAdapter = true)
internal data class AlgorandTransactionHistoryItem(
    @Json(name = "id") val id: String,
    @Json(name = "fee") val fee: Long,
    @Json(name = "round-time") val roundTime: Long?,
    @Json(name = "confirmed-round") val confirmedRound: Long?,
    @Json(name = "payment-transaction") val paymentTransaction: AlgorandPaymentTransaction?,
    @Json(name = "sender") val sender: String,
)

@JsonClass(generateAdapter = true)
internal data class AlgorandPaymentTransaction(
    @Json(name = "amount") val amount: Long,
    @Json(name = "receiver") val receiver: String,
)