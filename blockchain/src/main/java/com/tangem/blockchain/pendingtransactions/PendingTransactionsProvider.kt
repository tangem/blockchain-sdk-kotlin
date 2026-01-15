package com.tangem.blockchain.pendingtransactions

import com.tangem.blockchain.common.NetworkProvider

/**
 * Interface defining a provider for pending transactions management.
 * Provides methods to add, remove, and check pending transactions.
 */
interface PendingTransactionsProvider {

    /**
     * Adds a pending transaction to storage.
     *
     * @param transactionId Transaction hash (hex string)
     * @param networkProvider Provider type (null for public providers, specific provider for private mempool)
     * @param contractAddress Optional contract address (for token transactions)
     */
    suspend fun addPendingTransaction(
        transactionId: String,
        networkProvider: NetworkProvider,
        contractAddress: String? = null,
    )

    /**
     * Removes a pending transaction from storage.
     *
     * @param transactionId Transaction hash to remove
     */
    suspend fun removePendingTransaction(transactionId: String)

    /**
     * Gets current list of pending transaction IDs filtered by optional contract address.
     *
     * @param contractAddress Optional contract address to filter by
     * @return List of pending transaction IDs matching the criteria
     */
    suspend fun getPendingTransactions(contractAddress: String? = null): List<String>

    /**
     * Checks all pending transactions for the wallet and updates their status.
     * Removes executed or dropped transactions from storage.
     *
     * @return Map of transaction IDs to their current status
     */
    suspend fun checkPendingTransactions(): Map<String, PendingTransactionStatus>
}