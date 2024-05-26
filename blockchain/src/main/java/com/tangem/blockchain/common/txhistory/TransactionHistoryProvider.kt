package com.tangem.blockchain.common.txhistory

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.pagination.PaginationWrapper
import com.tangem.blockchain.extensions.Result

interface TransactionHistoryProvider {

    suspend fun getTransactionHistoryState(
        address: String,
        filterType: TransactionHistoryRequest.FilterType,
    ): TransactionHistoryState

    suspend fun getTransactionsHistory(
        request: TransactionHistoryRequest,
    ): Result<PaginationWrapper<TransactionHistoryItem>>

    // TODO: Move to inside mappers - [REDACTED_JIRA]
    fun shouldExcludeFromHistory(filterType: TransactionHistoryRequest.FilterType, amount: Amount): Boolean {
        return when (filterType) {
            TransactionHistoryRequest.FilterType.Coin -> return false
            is TransactionHistoryRequest.FilterType.Contract -> amount.value == null || amount.value.signum() == 0
        }
    }
}