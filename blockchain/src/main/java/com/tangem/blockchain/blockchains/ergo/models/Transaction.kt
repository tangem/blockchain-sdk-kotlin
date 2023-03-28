package com.tangem.blockchain.blockchains.ergo.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Transaction(
    @Json(name = "inputs")
    var inputs: List<Input>? = null,
    @Json(name = "outputs")
    var outputs: ArrayList<ErgoBox>? = null,
    @Json(name = "dataInputs")
    var dataInputs: ArrayList<Input>? = arrayListOf(),
    @Json(name = "timestamp")
    var timestamp: Long? = null,
    @Json(name = "confirmations")
    var confirmations: Long? = null,
    @Json(name = "headerId")
    var headerId: String? = null,
    @Json(name = "id")
    var id: String? = null,
)

fun Transaction.toHash(): ByteArray {
    return vlqEncode(this.inputs?.size ?: 0)
}

fun vlqEncode(n: Int): ByteArray {
    var num = n
    val numRelevantBits = 64 - n.countLeadingZeroBits()
    var numBytes = (numRelevantBits + 6) / 7
    if (numBytes == 0) numBytes = 1
    val output = ByteArray(numBytes)
    for (i in numBytes - 1 downTo 0) {
        var curByte = (num and 0x7F).toInt()
        if (i != numBytes - 1) curByte = curByte or 0x80
        output[i] = curByte.toByte()
        num = num ushr 7
    }
    return output
}
