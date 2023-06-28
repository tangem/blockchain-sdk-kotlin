package com.tangem.blockchain.common.txhistory

import com.tangem.blockchain.extensions.Result

internal object DefaultTransactionHistoryProvider : TransactionHistoryProvider {
    override suspend fun getTransactionHistoryState(address: String): TransactionHistoryState =
        TransactionHistoryState.NotImplemented

    override suspend fun getTransactionsHistory(
        address: String,
        page: Int,
        pageSize: Int,
    ): Result<List<TransactionHistoryItem>> = Result.Success(emptyList())
}
