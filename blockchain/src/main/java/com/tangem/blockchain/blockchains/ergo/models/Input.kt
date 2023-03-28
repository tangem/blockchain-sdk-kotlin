package com.tangem.blockchain.blockchains.ergo.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Input(
    @Json(name = "outputTransactionId")
    var outputTransactionId: String? = null,
    @Json(name = "value")
    var value: Long? = null,
    @Json(name = "address")
    var address: String? = null,
    @Json(name = "boxId")
    var boxId: String? = null,
    @Json(name = "spendingProof")
    var spendingProof: SpendingProof? = null
)

@JsonClass(generateAdapter = true)
data class SpendingProof(
    var proofBytes: String? = null,
    var extension: Map<String, Any>? = null

)
