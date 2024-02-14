package com.tangem.blockchain.common.txhistory

import com.tangem.blockchain.common.pagination.Page
import com.tangem.blockchain.common.pagination.PaginationWrapper
import com.tangem.blockchain.extensions.Result

internal object DefaultTransactionHistoryProvider : TransactionHistoryProvider {
    override suspend fun getTransactionHistoryState(
        address: String,
        filterType: TransactionHistoryRequest.FilterType,
    ): TransactionHistoryState = TransactionHistoryState.NotImplemented

    override suspend fun getTransactionsHistory(
        request: TransactionHistoryRequest,
    ): Result<PaginationWrapper<TransactionHistoryItem>> = Result.Success(
        PaginationWrapper(
            nextPage = Page.LastPage,
            items = emptyList(),
        ),
    )
}