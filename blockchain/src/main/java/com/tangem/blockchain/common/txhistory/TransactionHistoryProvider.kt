package com.tangem.blockchain.common.txhistory

import com.tangem.blockchain.common.PaginationWrapper
import com.tangem.blockchain.extensions.Result

interface TransactionHistoryProvider {

    suspend fun getTransactionHistoryState(address: String, filterType: TransactionHistoryRequest.FilterType): TransactionHistoryState

    suspend fun getTransactionsHistory(request: TransactionHistoryRequest): Result<PaginationWrapper<TransactionHistoryItem>>
}
