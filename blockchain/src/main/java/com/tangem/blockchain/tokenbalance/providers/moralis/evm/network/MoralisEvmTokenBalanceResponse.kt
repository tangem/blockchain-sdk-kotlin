package com.tangem.blockchain.tokenbalance.providers.moralis.evm.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class MoralisEvmTokenBalanceResponse(
    @Json(name = "result") val result: List<MoralisEvmTokenBalanceItem>,
    @Json(name = "cursor") val cursor: String?,
)

@JsonClass(generateAdapter = true)
internal data class MoralisEvmTokenBalanceItem(
    @Json(name = "token_address") val tokenAddress: String?,
    @Json(name = "name") val name: String,
    @Json(name = "symbol") val symbol: String,
    @Json(name = "decimals") val decimals: Int,
    @Json(name = "balance") val balance: String,
    @Json(name = "balance_formatted") val balanceFormatted: String,
    @Json(name = "possible_spam") val isPossibleSpam: Boolean,
    @Json(name = "verified_contract") val isVerifiedContract: Boolean?,
    @Json(name = "native_token") val isNativeToken: Boolean,
    @Json(name = "logo") val logo: String?,
)