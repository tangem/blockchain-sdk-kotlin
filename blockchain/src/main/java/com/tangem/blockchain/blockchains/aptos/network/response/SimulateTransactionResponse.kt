package com.tangem.blockchain.blockchains.aptos.network.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SimulateTransactionResponse(
    @Json(name = "gas_used") val usedGasUnit: String,
    @Json(name = "sequence_number") val sequenceNumber: String,
    @Json(name = "success") val isSuccess: Boolean,
)