package com.tangem.blockchain.blockchains.ton.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class TonProviderResponse<T>(
    @Json(name = "ok") val ok: Boolean,
    @Json(name = "result") val result: T,
    @Json(name = "error") val error: String? = null,
    @Json(name = "code") val code: Int? = null,
)

/**
 * @param wallet - Is chain transaction wallet
 * @param seqno - Sequence number transactions
 */
@JsonClass(generateAdapter = true)
data class TonGetWalletInfoResponse(
    @Json(name = "wallet") val wallet: Boolean,
    @Json(name = "balance") val balance: BigDecimal,
    @Json(name = "account_state") val accountState: TonAccountState,
    @Json(name = "seqno") val seqno: Int?,
)

enum class TonAccountState {
    @Json(name = "active")
    ACTIVE,

    @Json(name = "uninitialized")
    UNINITIALIZED,
}

@JsonClass(generateAdapter = true)
data class TonGetFeeResponse(
    @Json(name = "source_fees") val sourceFees: TonSourceFees,
)

/**
 * @param inFwdFee - Is a charge for importing messages from outside the blockchain. Every time you make a transaction,
 * it must be delivered to the validators who will process it.
 * @param storageFee - Is the amount you pay for storing a smart contract in the blockchain. In fact, you pay for every
 * second the smart contract is stored on the blockchain.
 * @param gasFee - Is the amount you pay for executing code in the virtual machine. The larger the code, the more fees
 * must be paid.
 * @param fwdFee - Stands for a charge for sending messages outside the TON.
 */
@JsonClass(generateAdapter = true)
data class TonSourceFees(
    @Json(name = "in_fwd_fee") val inFwdFee: BigDecimal,
    @Json(name = "storage_fee") val storageFee: BigDecimal,
    @Json(name = "gas_fee") val gasFee: BigDecimal,
    @Json(name = "fwd_fee") val fwdFee: BigDecimal,
) {
    val totalFee: BigDecimal
        get() = inFwdFee + storageFee + gasFee + fwdFee
}

/**
 * @param hash - Transaction hash.
 */
@JsonClass(generateAdapter = true)
data class TonSendBocResponse(
    @Json(name = "hash") val hash: String,
)
