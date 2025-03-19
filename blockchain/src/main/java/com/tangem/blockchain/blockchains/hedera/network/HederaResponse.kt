package com.tangem.blockchain.blockchains.hedera.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
internal data class HederaAccountResponse(
    @Json(name = "accounts")
    val accounts: List<HederaAccount>,
)

@JsonClass(generateAdapter = true)
internal data class HederaExchangeRateResponse(
    @Json(name = "current_rate")
    val currentRate: HederaRate,

    @Json(name = "next_rate")
    val nextRate: HederaRate,
)

internal data class HederaAccount(
    @Json(name = "account")
    val account: String,
)

@JsonClass(generateAdapter = true)
internal data class HederaRate(
    @Json(name = "cent_equivalent")
    val centEquivalent: String,

    @Json(name = "hbar_equivalent")
    val hbarEquivalent: String,

    @Json(name = "expiration_time")
    val expirationTime: String,
)

@JsonClass(generateAdapter = true)
internal data class HederaBalancesResponse(@Json(name = "balances") val balances: List<HederaBalanceResponse>)

@JsonClass(generateAdapter = true)
internal data class HederaBalanceResponse(
    @Json(name = "account")
    val account: String,

    @Json(name = "balance")
    val balance: Long,

    @Json(name = "tokens")
    val tokenBalances: List<HederaTokenBalanceResponse>,
)

@JsonClass(generateAdapter = true)
internal data class HederaTokenBalanceResponse(
    @Json(name = "token_id")
    val tokenId: String,

    @Json(name = "balance")
    val balance: Long,
)

@JsonClass(generateAdapter = true)
internal data class HederaTransactionsResponse(
    @Json(name = "transactions")
    val transactions: List<HederaTransactionResponse>,
)

@JsonClass(generateAdapter = true)
internal data class HederaTransactionResponse(
    @Json(name = "transaction_hash")
    val transactionHash: String,

    @Json(name = "transaction_id")
    val transactionId: String,

    @Json(name = "result")
    val result: String,
)

@JsonClass(generateAdapter = true)
data class HederaTokenDetailsResponse(
    // / there are more fields available
    // / on the /tokens/{token_id} response, ignore for now
    @Json(name = "custom_fees")
    val customFees: CustomFees?,
)

@JsonClass(generateAdapter = true)
data class CustomFees(
    @Json(name = "created_timestamp")
    val createdTimestamp: String?,

    @Json(name = "fixed_fees")
    val fixedFees: List<CustomFixedFee>,

    @Json(name = "fractional_fees")
    val fractionalFees: List<CustomFractionalFee>,

    @Json(name = "royalty_fees")
    val royaltyFees: List<CustomRoyaltyFee>?,
) {
    @JsonClass(generateAdapter = true)
    data class CustomFixedFee(
        @Json(name = "collector_account_id")
        val collectorAccountId: String?,

        @Json(name = "all_collectors_are_exempt")
        val allCollectorsAreExempt: Boolean?,

        @Json(name = "amount")
        val amount: BigDecimal?,

        @Json(name = "denominating_token_id")
        val denominatingTokenId: String?,
    )

    @JsonClass(generateAdapter = true)
    data class CustomFractionalFee(
        @Json(name = "collector_account_id")
        val collectorAccountId: String?,

        @Json(name = "all_collectors_are_exempt")
        val allCollectorsAreExempt: Boolean?,

        @Json(name = "amount")
        val amount: Amount?,

        @Json(name = "maximum")
        val maximum: BigDecimal?,

        @Json(name = "minimum")
        val minimum: BigDecimal?,

        @Json(name = "denominating_token_id")
        val denominatingTokenId: String?,

        @Json(name = "net_of_transfers")
        val netOfTransfers: Boolean?,
    )

    @JsonClass(generateAdapter = true)
    data class CustomRoyaltyFee(
        @Json(name = "collector_account_id")
        val collectorAccountId: String?,

        @Json(name = "all_collectors_are_exempt")
        val allCollectorsAreExempt: Boolean?,

        @Json(name = "amount")
        val amount: Amount?,

        @Json(name = "denominating_token_id")
        val denominatingTokenId: String?,

        @Json(name = "fallback_fee")
        val fallbackFee: FixedFee?,
    )

    @JsonClass(generateAdapter = true)
    data class Amount(
        @Json(name = "numerator")
        val numerator: Int?,

        @Json(name = "denominator")
        val denominator: Int?,
    )

    @JsonClass(generateAdapter = true)
    data class FixedFee(
        @Json(name = "amount")
        val amount: BigDecimal?,

        @Json(name = "denominating_token_id")
        val denominatingTokenId: String?,
    )
}