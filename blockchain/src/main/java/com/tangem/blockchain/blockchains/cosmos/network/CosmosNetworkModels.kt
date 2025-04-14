package com.tangem.blockchain.blockchains.cosmos.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CosmosAccountResponse(
    @Json(name = "account") val account: CosmosAccount,
)

@JsonClass(generateAdapter = true)
data class CosmosAccount(
    @Json(name = "account_number") val accountNumber: Long,
    @Json(name = "sequence") val sequence: Long,
)

@JsonClass(generateAdapter = true)
data class CosmosBalanceResponse(
    @Json(name = "balances") val balances: List<CosmosBalance>,
)

@JsonClass(generateAdapter = true)
data class CosmosBalance(
    @Json(name = "denom") val denom: String,
    @Json(name = "amount") val amount: Long,
)

@JsonClass(generateAdapter = true)
data class CosmosSendTransactionRequest(
    @Json(name = "mode") val mode: String,
    @Json(name = "tx_bytes") val txBytes: String,
)

@JsonClass(generateAdapter = true)
data class CosmosSimulateResponse(
    @Json(name = "gas_info") val gasInfo: CosmosGasInfo,
)

@JsonClass(generateAdapter = true)
data class CosmosGasInfo(
    @Json(name = "gas_used") val gasUsed: Long,
)

@JsonClass(generateAdapter = true)
data class CosmosTxResponse(
    @Json(name = "tx_response") val txInfo: CosmosTxInfo,
)

@JsonClass(generateAdapter = true)
data class CosmosTxInfo(
    @Json(name = "height") val height: Long,
    @Json(name = "txhash") val txhash: String,
    @Json(name = "code") val code: Int,
)

@JsonClass(generateAdapter = true)
data class CosmosErrorResponse(
    @Json(name = "code") val code: Int,
    @Json(name = "message") val message: String,
)