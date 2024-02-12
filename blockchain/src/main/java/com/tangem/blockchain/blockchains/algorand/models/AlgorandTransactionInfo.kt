package com.tangem.blockchain.blockchains.algorand.models

internal data class AlgorandTransactionInfo(
    val transactionHash: String?,
    val status: Status,
) {
    enum class Status {
        COMMITTED,
        STILL,
        REMOVED,
    }
}