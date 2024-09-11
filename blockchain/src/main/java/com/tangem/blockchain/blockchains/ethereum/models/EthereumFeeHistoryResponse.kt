package com.tangem.blockchain.blockchains.ethereum.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class EthereumFeeHistoryResponse(
    @Json(name = "baseFeePerGas") val baseFeePerGas: List<String>,
    @Json(name = "reward") val reward: List<List<String>>,
)