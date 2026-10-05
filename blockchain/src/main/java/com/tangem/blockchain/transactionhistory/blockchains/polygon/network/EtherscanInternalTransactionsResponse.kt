package com.tangem.blockchain.transactionhistory.blockchains.polygon.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class EtherscanInternalTransactionsResponse(
    @Json(name = "status") val status: String?,
    @Json(name = "message") val message: String?,
    @Json(name = "result") val result: EtherscanInternalResult,
)

internal sealed class EtherscanInternalResult {

    internal data class Description(val description: String) : EtherscanInternalResult()

    internal data class Transactions(val txs: List<EtherscanInternalTransaction>) : EtherscanInternalResult()
}

/** No `confirmations`, `gasPrice`, `functionName`: an internal call is always mined and has no calldata here */
@JsonClass(generateAdapter = true)
internal data class EtherscanInternalTransaction(
    @Json(name = "blockNumber") val blockNumber: String,
    @Json(name = "timeStamp") val timeStamp: String,
    @Json(name = "hash") val hash: String,
    @Json(name = "from") val from: String,
    @Json(name = "to") val to: String,
    @Json(name = "value") val value: String,
    @Json(name = "type") val type: String,
    @Json(name = "isError") val isError: String?,
)