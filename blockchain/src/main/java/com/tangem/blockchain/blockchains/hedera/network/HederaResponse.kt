package com.tangem.blockchain.blockchains.hedera.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HederaAccountResponse(
    @Json(name = "accounts")
    val accounts: List<HederaAccount>,
)

@JsonClass(generateAdapter = true)
data class HederaExchangeRateResponse(
    @Json(name = "current_rate")
    val currentRate: HederaRate,

    @Json(name = "next_rate")
    val nextRate: HederaRate,
)

data class HederaAccount(
    @Json(name = "account")
    val account: String,
)

@JsonClass(generateAdapter = true)
data class HederaRate(
    @Json(name = "cent_equivalent")
    val centEquivalent: String,

    @Json(name = "hbar_equivalent")
    val hbarEquivalent: String,

    @Json(name = "expiration_time")
    val expirationTime: String,
)