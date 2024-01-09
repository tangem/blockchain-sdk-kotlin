package com.tangem.blockchain.blockchains.vechain.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class VechainGetAccountResponse(
    @Json(name = "balance") val balance: String,
    @Json(name = "energy") val energy: String,
)

@JsonClass(generateAdapter = true)
internal data class VechainLatestBlockResponse(
    @Json(name = "number") val number: Long,
    @Json(name = "id") val blockId: String,
)

@JsonClass(generateAdapter = true)
internal data class VechainCommitTransactionRequest(@Json(name = "raw") val raw: String)

@JsonClass(generateAdapter = true)
internal data class VechainCommitTransactionResponse(@Json(name = "id") val txId: String)

@JsonClass(generateAdapter = true)
internal data class VechainTransactionInfoResponse(@Json(name = "id") val txId: String)