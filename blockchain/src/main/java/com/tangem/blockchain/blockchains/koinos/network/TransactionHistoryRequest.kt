package com.tangem.blockchain.blockchains.koinos.network

internal data class TransactionHistoryRequest(
    val address: String,
    val pageSize: Int,
    val sequenceNum: Long,
)