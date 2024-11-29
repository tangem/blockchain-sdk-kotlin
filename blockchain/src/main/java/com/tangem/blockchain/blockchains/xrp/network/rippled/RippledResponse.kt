package com.tangem.blockchain.blockchains.xrp.network.rippled

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Rippled account
@JsonClass(generateAdapter = true)
data class RippledAccountResponse(
    @Json(name = "result")
    val result: RippledAccountResult? = null,
)

@JsonClass(generateAdapter = true)
data class RippledAccountResult(
    @Json(name = "account_data")
    val accountData: RippledAccountData? = null,

    @Json(name = "error_code")
    val errorCode: Int? = null,
)

@JsonClass(generateAdapter = true)
data class RippledAccountData(
    @Json(name = "Balance")
    val balance: String? = null,

    @Json(name = "Sequence")
    val sequence: Long? = null,

    @Json(name = "OwnerCount")
    val ownerCount: Long? = null,
)

// Rippled state
@JsonClass(generateAdapter = true)
data class RippledStateResponse(
    @Json(name = "result")
    val result: RippledStateResult? = null,
)

@JsonClass(generateAdapter = true)
data class RippledStateResult(
    @Json(name = "state")
    val state: RippledState? = null,
)

@JsonClass(generateAdapter = true)
data class RippledState(
    @Json(name = "validated_ledger")
    val validatedLedger: RippledLedger? = null,
)

@JsonClass(generateAdapter = true)
data class RippledLedger(
    @Json(name = "reserve_base")
    val reserveBase: Long? = null,
    @Json(name = "reserve_inc")
    val reserveInc: Long? = null,
)

// Rippled fee
@JsonClass(generateAdapter = true)
data class RippledFeeResponse(
    @Json(name = "result")
    val result: RippledFeeResult? = null,
)

@JsonClass(generateAdapter = true)
data class RippledFeeResult(
    @Json(name = "drops")
    val feeData: RippledFeeData? = null,
)

@JsonClass(generateAdapter = true)
data class RippledFeeData(
    // enough to put tx to queue
    @Json(name = "minimum_fee")
    val minimalFee: String? = null,

    // enough to put tx to current ledger
    @Json(name = "open_ledger_fee")
    val normalFee: String? = null,

    @Json(name = "median_fee")
    val priorityFee: String? = null,
)

// Rippled submit
@JsonClass(generateAdapter = true)
data class RippledSubmitResponse(
    @Json(name = "result")
    val result: RippledSubmitResult? = null,
)

@JsonClass(generateAdapter = true)
data class RippledSubmitResult(
    @Json(name = "engine_result_code")
    val resultCode: Int? = null,

    @Json(name = "engine_result_message")
    val resultMessage: String? = null,

    @Json(name = "error")
    val error: String? = null,

    @Json(name = "error_exception")
    val errorException: String? = null,
)