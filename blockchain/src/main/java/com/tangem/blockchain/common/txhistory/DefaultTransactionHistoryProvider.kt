package com.tangem.blockchain.common.txhistory

import com.tangem.blockchain.common.PaginationWrapper
import com.tangem.blockchain.extensions.Result

internal object DefaultTransactionHistoryProvider : TransactionHistoryProvider {
    override suspend fun getTransactionHistoryState(
        address: String,
        filterType: TransactionHistoryRequest.FilterType,
    ): TransactionHistoryState = TransactionHistoryState.NotImplemented

    override suspend fun getTransactionsHistory(
        request: TransactionHistoryRequest
    ): Result<PaginationWrapper<TransactionHistoryItem>> = Result.Success(
        PaginationWrapper(
            page = 0,
            totalPages = 0,
            itemsOnPage = 0,
            items = emptyList()
        )
    )
}
