package com.tangem.blockchain.transactionhistory

import com.tangem.blockchain.common.pagination.PaginationWrapper
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryRequest
import com.tangem.common.extensions.isZero

interface TransactionHistoryProvider {

    suspend fun getTransactionHistoryState(
        address: String,
        filterType: TransactionHistoryRequest.FilterType,
    ): TransactionHistoryState

    suspend fun getTransactionsHistory(
        request: TransactionHistoryRequest,
    ): Result<PaginationWrapper<TransactionHistoryItem>>

    companion object {

        fun shouldExcludeFromHistory(
            filterType: TransactionHistoryRequest.FilterType,
            item: TransactionHistoryItem,
        ): Boolean {
            val amount = item.amount.value
            val isCoinHistory = filterType == TransactionHistoryRequest.FilterType.Coin
            val isPlainTransfer = item.type == TransactionHistoryItem.TransactionType.Transfer

            return when {
                item.status == TransactionHistoryItem.TransactionStatus.Failed -> false
                isCoinHistory && !isPlainTransfer -> false
                else -> amount == null || amount.isZero()
            }
        }
    }
}