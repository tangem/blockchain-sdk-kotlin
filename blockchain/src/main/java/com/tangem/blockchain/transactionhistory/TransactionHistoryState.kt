package com.tangem.blockchain.transactionhistory

sealed interface TransactionHistoryState {

    sealed interface Success : TransactionHistoryState {
        object Empty : Success
        data class HasTransactions(val txCount: Int) : Success
    }

    sealed interface Failed : TransactionHistoryState {
        data class FetchError(val exception: Exception) : Failed
    }

    object NotImplemented : TransactionHistoryState
}