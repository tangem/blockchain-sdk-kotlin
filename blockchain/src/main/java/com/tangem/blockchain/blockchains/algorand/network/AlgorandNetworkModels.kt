package com.tangem.blockchain.blockchains.algorand.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class AlgorandAccountResponse(
    @Json(name = "amount") val amount: Long,
    @Json(name = "min-balance") val minBalance: Long,
)

@JsonClass(generateAdapter = true)
internal data class AlgorandTransactionParamsResponse(
    @Json(name = "genesis-id") val genesisId: String,
    @Json(name = "genesis-hash") val genesisHash: String,
    @Json(name = "consensus-version") val consensusVersion: String,
    @Json(name = "fee") val fee: Long,
    @Json(name = "last-round") val lastRound: Long,
    @Json(name = "min-fee") val minFee: Long,
)

@JsonClass(generateAdapter = true)
internal data class AlgorandTransactionResultResponse(@Json(name = "txId") val txId: String)

@JsonClass(generateAdapter = true)
internal data class AlgorandPendingTransactionResponse(
    @Json(name = "confirmed-round") val confirmedRound: Long?,
    @Json(name = "pool-error") val poolError: String,
)

// region TransactionHistory
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
// endregion