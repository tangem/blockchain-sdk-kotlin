package com.tangem.blockchain.transaction.staking.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class TronStakingRawTx(
    @Json(name = "raw_data_hex") val rawDataHex: String?,
)