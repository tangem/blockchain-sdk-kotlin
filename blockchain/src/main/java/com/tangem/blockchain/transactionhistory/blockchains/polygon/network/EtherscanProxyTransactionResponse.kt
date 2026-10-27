package com.tangem.blockchain.transactionhistory.blockchains.polygon.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.blockchain.blockchains.ethereum.network.EthereumTransactionResponse

@JsonClass(generateAdapter = true)
internal data class EtherscanProxyTransactionResponse(
    @Json(name = "result") val result: EthereumTransactionResponse?,
)