package com.tangem.blockchain.blockchains.aptos.network.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class AptosSubmitTransactionResponse(
    @Json(name = "hash") val hash: String,
    @Json(name = "sequence_number") val sequenceNumber: String,
)
