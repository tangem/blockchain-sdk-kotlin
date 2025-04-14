package com.tangem.blockchain.blockchains.koinos.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class KoinosContractResponse(
    // there are more fields available
    // in this response, ignore for now
    @Json(name = "contract_id") val contractId: String,
)