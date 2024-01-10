package com.tangem.blockchain.network.blockbook.network.responses

data class GetUtxoResponseItem(
    val confirmations: Int,
    val height: Int?,
    val txid: String,
    val value: String,
    val vout: Int,
)