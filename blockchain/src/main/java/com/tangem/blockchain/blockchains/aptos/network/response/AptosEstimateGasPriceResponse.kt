package com.tangem.blockchain.blockchains.aptos.network.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class AptosEstimateGasPriceResponse(
    @Json(name = "deprioritized_gas_estimate") val minimalGasUnitPrice: Long,
    @Json(name = "gas_estimate") val normalGasUnitPrice: Long,
    @Json(name = "prioritized_gas_estimate") val priorityGasUnitPrice: Long,
)
