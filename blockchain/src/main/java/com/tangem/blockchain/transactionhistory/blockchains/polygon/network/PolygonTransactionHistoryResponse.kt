package com.tangem.blockchain.transactionhistory.blockchains.polygon.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class PolygonTransactionHistoryResponse(
    @Json(name = "status") val status: String?,
    @Json(name = "message") val message: String?,
    @Json(name = "result") val result: PolygonScanResult,
)

internal sealed class PolygonScanResult {

    internal data class Description(val description: String) : PolygonScanResult()

    @JsonClass(generateAdapter = true)
    internal data class Transactions(
        @Json(name = "txs")
        val txs: List<PolygonTransaction>,
    ) : PolygonScanResult()

    val transactions: List<PolygonTransaction>?
        get() = when (this) {
            is Transactions -> this.txs
            else -> null
        }
}

@JsonClass(generateAdapter = true)
internal data class PolygonTransaction(
    @Json(name = "confirmations") val confirmations: String,
    @Json(name = "contractAddress") val contractAddress: String?,
    @Json(name = "from") val from: String,
    @Json(name = "functionName") val functionName: String?,
    @Json(name = "methodId") val methodId: String?,
    @Json(name = "gasPrice") val gasPrice: String,
    @Json(name = "gasUsed") val gasUsed: String,
    @Json(name = "hash") val hash: String,
    @Json(name = "isError") val isError: String?,
    @Json(name = "timeStamp") val timeStamp: String,
    @Json(name = "to") val to: String,
    @Json(name = "txreceipt_status") val txReceiptStatus: String?,
    @Json(name = "value") val value: String,
)