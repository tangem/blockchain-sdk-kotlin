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

@JsonClass(generateAdapter = true)
internal data class AlgorandErrorResponse(@Json(name = "message") val message: String?)