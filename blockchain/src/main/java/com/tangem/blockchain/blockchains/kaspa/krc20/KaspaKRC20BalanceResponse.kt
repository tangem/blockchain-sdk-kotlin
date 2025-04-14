package com.tangem.blockchain.blockchains.kaspa.krc20

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KaspaKRC20BalanceResponse(
    @Json(name = "result")
    val result: List<TokenBalance> = emptyList(),
) {
    @JsonClass(generateAdapter = true)
    data class TokenBalance(
        @Json(name = "balance")
        val balance: Long? = null,
    )
}