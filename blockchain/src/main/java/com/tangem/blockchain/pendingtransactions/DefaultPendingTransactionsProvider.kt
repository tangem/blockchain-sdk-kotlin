package com.tangem.blockchain.pendingtransactions

import com.tangem.blockchain.common.NetworkProvider

/**
 * Default implementation of [PendingTransactionsProvider] that does nothing.
 * Used when pending transactions feature is not supported or disabled.
 */
internal object DefaultPendingTransactionsProvider : PendingTransactionsProvider {

    override suspend fun addPendingTransaction(
        transactionId: String,
        networkProvider: NetworkProvider,
        contractAddress: String?,
    ) {
        // No-op
    }

    override suspend fun addPendingGaslessTransaction(transactionId: String, contractAddress: String?) {
        // No-op
    }

    override suspend fun removePendingTransaction(transactionId: String) {
        // No-op
    }

    override suspend fun getPendingTransactions(contractAddress: String?): List<String> = emptyList()

    override suspend fun checkPendingTransactions(): Map<String, PendingTransactionStatus> = emptyMap()
}