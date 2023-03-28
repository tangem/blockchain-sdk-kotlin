package com.tangem.blockchain.blockchains.ergo.network.api.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ErgoApiTransactionRespone(
    @Json(name = "summary")
    var summary: Summary? = Summary(),
    @Json(name = "ioSummary")
    var ioSummary: IoSummary? = IoSummary(),
    @Json(name = "inputs")
    var inputs: ArrayList<Inputs> = arrayListOf(),
    @Json(name = "dataInputs")
    var dataInputs: ArrayList<String> = arrayListOf(),
    @Json(name = "outputs")
    var outputs: ArrayList<Outputs> = arrayListOf(),
)

@JsonClass(generateAdapter = true)
data class Block(
    @Json(name = "id")
    var id: String? = null,
    @Json(name = "height")
    var height: Int? = null,
)

@JsonClass(generateAdapter = true)
data class Summary(
    @Json(name = "id")
    var id: String? = null,
    @Json(name = "timestamp")
    var timestamp: Int? = null,
    @Json(name = "index")
    var index: Int? = null,
    @Json(name = "size")
    var size: Int? = null,
    @Json(name = "confirmationsCount")
    var confirmationsCount: Int? = null,
    @Json(name = "block")
    var block: Block? = Block(),
)

@JsonClass(generateAdapter = true)
data class IoSummary(
    @Json(name = "totalCoinsTransferred")
    var totalCoinsTransferred: Int? = null,
    @Json(name = "totalFee")
    var totalFee: Int? = null,
    @Json(name = "feePerByte")
    var feePerByte: Double? = null,
)

@JsonClass(generateAdapter = true)
data class Inputs(
    @Json(name = "id")
    var id: String? = null,
    @Json(name = "value")
    var value: Int? = null,
    @Json(name = "index")
    var index: Int? = null,
    @Json(name = "spendingProof")
    var spendingProof: String? = null,
    @Json(name = "transactionId")
    var transactionId: String? = null,
    @Json(name = "outputTransactionId")
    var outputTransactionId: String? = null,
    @Json(name = "outputIndex")
    var outputIndex: Int? = null,
    @Json(name = "address")
    var address: String? = null,
)

@JsonClass(generateAdapter = true)
data class Outputs(
    @Json(name = "id")
    var id: String? = null,
    @Json(name = "txId")
    var txId: String? = null,
    @Json(name = "value")
    var value: Int? = null,
    @Json(name = "index")
    var index: Int? = null,
    @Json(name = "creationHeight")
    var creationHeight: Int? = null,
    @Json(name = "ergoTree")
    var ergoTree: String? = null,
    @Json(name = "address")
    var address: String? = null,
    @Json(name = "assets")
    var assets: ArrayList<String> = arrayListOf(),
    @Json(name = "spentTransactionId")
    var spentTransactionId: String? = null,
    @Json(name = "mainChain")
    var mainChain: Boolean? = null,
)
