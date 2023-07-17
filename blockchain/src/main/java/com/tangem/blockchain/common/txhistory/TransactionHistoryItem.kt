package com.tangem.blockchain.common.txhistory

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionStatus

data class TransactionHistoryItem(
    val txHash: String,
    val timestamp: Long,
    val direction: TransactionDirection,
    val status: TransactionStatus,
    val type: TransactionType,
    val amount: Amount,
) {
    sealed interface TransactionDirection {
        data class Incoming(val from: String) : TransactionDirection
        data class Outgoing(val to: String) : TransactionDirection
    }

    sealed interface TransactionType {
        object Transfer : TransactionType
    }
}
