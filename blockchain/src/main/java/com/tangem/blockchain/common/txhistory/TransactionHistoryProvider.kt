package com.tangem.blockchain.common.txhistory

import com.tangem.blockchain.common.PaginationWrapper
import com.tangem.blockchain.extensions.Result

interface TransactionHistoryProvider {

    suspend fun getTransactionHistoryState(address: String): TransactionHistoryState

    suspend fun getTransactionsHistory(
        address: String,
        page: Int,
        pageSize: Int,
    ): Result<PaginationWrapper<TransactionHistoryItem>>
}
