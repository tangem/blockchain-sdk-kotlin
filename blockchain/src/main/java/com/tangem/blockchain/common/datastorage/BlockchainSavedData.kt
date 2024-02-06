package com.tangem.blockchain.common.datastorage

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

internal sealed interface BlockchainSavedData {

    @JsonClass(generateAdapter = true)
    data class Hedera(
        @Json(name = "address") val address: String,
    ) : BlockchainSavedData
}
