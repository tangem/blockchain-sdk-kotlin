package com.tangem.blockchain.blockchains.ergo.network.api.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class ErgoApiMempoolResponse (
    @Json(name = "items")
    var items: List<Item>? = null,

    @Json(name = "total")
    var total: Long? = null
)

@JsonClass(generateAdapter = true)
data class Item (
    @Json(name = "id")
    var id: String? = null,

    @Json(name = "creationTimestamp")
    var creationTimestamp: Long? = null,

    @Json(name = "inputs")
    var inputs: List<Input>? = null,

    @Json(name = "dataInputs")
    var dataInputs: List<Any>? = null,

    @Json(name = "outputs")
    var outputs: List<Output>? = null,

    @Json(name = "size")
    var size: Long? = null
)

@JsonClass(generateAdapter = true)
data class Output (
    @Json(name = "boxId")
    var boxId: String? = null,

    @Json(name = "transactionId")
    var transactionId: String? = null,

    @Json(name = "value")
    var value: Long? = null,

    @Json(name = "index")
    var index: Long? = null,

    @Json(name = "creationHeight")
    var creationHeight: Long? = null,

    @Json(name = "ergoTree")
    var ergoTree: String? = null,

    @Json(name = "address")
    var address: String? = null,

    @Json(name = "assets")
    var assets: List<Any>? = null,

    @Json(name = "spentTransactionId")
    var spentTransactionId: String? = null,
)

@JsonClass(generateAdapter = true)
data class Input (
    @Json(name = "boxId")
    var boxId: String? = null,

    @Json(name = "value")
    var value: Long? = null,

    @Json(name = "index")
    var index: Long? = null,

    @Json(name = "spendingProof")
    var spendingProof: String? = null,

    @Json(name = "outputBlockId")
    var outputBlockId: String? = null,

    @Json(name = "outputTransactionId")
    var outputTransactionId: String? = null,

    @Json(name = "outputIndex")
    var outputIndex: Long? = null,

    @Json(name = "ergoTree")
    var ergoTree: String? = null,

    @Json(name = "address")
    var address: String? = null,

    @Json(name = "assets")
    var assets: List<Any>? = null
)
