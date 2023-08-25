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

        val address: Address

        data class Incoming(override val address: Address) : TransactionDirection
        data class Outgoing(override val address: Address) : TransactionDirection
    }

    sealed interface TransactionType {
        object Transfer : TransactionType
    }

    sealed class Address {
        data class Single(val rawAddress: String) : Address()
        object Multiple : Address()
    }
}