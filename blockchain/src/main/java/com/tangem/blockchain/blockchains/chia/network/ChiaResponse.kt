package com.tangem.blockchain.blockchains.chia.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChiaCoinRecordsResponse(
    @Json(name = "coin_records")
    val coinRecords: List<ChiaCoinRecord>
)

@JsonClass(generateAdapter = true)
data class ChiaEstimateFeeResponse(
    val estimates: List<Long>
)

@JsonClass(generateAdapter = true)
data class ChiaSendTransactionResponse(
    val success: Boolean,
    val status: String?,
    val error: String?
)

@JsonClass(generateAdapter = true)
data class ChiaCoinRecord(
    val coin: ChiaCoin
)