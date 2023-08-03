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
    @Json(name = "estimates")
    val estimates: List<Long>
)

@JsonClass(generateAdapter = true)
data class ChiaSendTransactionResponse(
    @Json(name = "success")
    val success: Boolean,

    @Json(name = "status")
    val status: String?,

    @Json(name = "error")
    val error: String?
)

@JsonClass(generateAdapter = true)
data class ChiaCoinRecord(
    @Json(name = "coin")
    val coin: ChiaCoin
)