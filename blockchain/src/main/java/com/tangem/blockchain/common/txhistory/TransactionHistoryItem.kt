package com.tangem.blockchain.common.txhistory

import com.tangem.blockchain.common.Amount

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
        object Submit : TransactionType
        object Approve : TransactionType
        object Supply : TransactionType
        object Withdraw : TransactionType
        object Deposit : TransactionType
        object Swap : TransactionType
        object Unoswap : TransactionType
        data class Custom(val id: String) : TransactionType
    }

    sealed class TransactionStatus {
        object Failed : TransactionStatus()
        object Unconfirmed : TransactionStatus()
        object Confirmed : TransactionStatus()
    }

    sealed class Address {
        data class Single(val rawAddress: String) : Address()
        object Multiple : Address()
    }
}