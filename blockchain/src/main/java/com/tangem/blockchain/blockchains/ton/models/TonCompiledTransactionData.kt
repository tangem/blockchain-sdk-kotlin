package com.tangem.blockchain.blockchains.ton.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TonCompiledTransactionData(
    @Json(name = "seqno") val seqno: Int,
    @Json(name = "message") val message: String,
)