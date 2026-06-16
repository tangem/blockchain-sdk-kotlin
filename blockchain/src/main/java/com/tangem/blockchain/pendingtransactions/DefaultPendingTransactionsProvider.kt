package com.tangem.blockchain.pendingtransactions

import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.datastorage.PendingTransaction

/**
 * Default implementation of [PendingTransactionsProvider] that does nothing.
 * Used when pending transactions feature is not supported or disabled.
 */
internal object DefaultPendingTransactionsProvider : PendingTransactionsProvider {

    override suspend fun addPendingTransaction(
        transactionId: String,
        networkProvider: NetworkProvider,
        transactionData: TransactionData,
    ) {
        // No-op
    }

    override suspend fun addPendingGaslessTransaction(transactionId: String, transactionData: TransactionData) {
        // No-op
    }

    override suspend fun removePendingTransaction(transactionId: String) {
        // No-op
    }

    override suspend fun getPendingTransactions(contractAddress: String?): List<PendingTransaction> = emptyList()

    override suspend fun checkPendingTransactions(): Map<String, PendingTransactionStatus> = emptyMap()
}