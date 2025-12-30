package com.tangem.blockchain.pendingtransactions

import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.datastorage.BlockchainSavedData.PendingTransactions
import com.tangem.blockchain.common.datastorage.PendingTransaction
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
import com.tangem.blockchain.common.logging.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Storage class for managing pending transactions.
 * Provides thread-safe operations for adding, removing, and retrieving pending transactions.
 *
 * @property wallet Wallet instance
 * @property dataStorage Advanced data storage for persisting pending transactions
 */
internal class PendingTransactionStorage(
    private val wallet: Wallet,
    private val dataStorage: AdvancedDataStorage,
) {

    private val mutex = Mutex()

    /**
     * Adds a pending transaction to storage.
     *
     * @param transactionId Transaction hash (hex string)
     * @param providerName Provider name (null for public providers, specific name for private mempool)
     * @param contractAddress Optional contract address (for token transactions)
     */
    suspend fun addTransaction(transactionId: String, providerName: String? = null, contractAddress: String? = null) {
        mutex.withLock {
            val currentData = getPendingTransactionsData()
            val newTransaction = PendingTransaction(
                transactionId = transactionId,
                blockchain = wallet.blockchain.id,
                providerName = providerName,
                sentAt = System.currentTimeMillis(),
                contractAddress = contractAddress,
            )

            val updatedTransactions = if (currentData.transactions.any { it.transactionId == transactionId }) {
                currentData.transactions
            } else {
                currentData.transactions + newTransaction
            }

            val updatedData = currentData.copy(transactions = updatedTransactions)
            dataStorage.store(wallet.publicKey, updatedData)
        }
    }

    /**
     * Removes a pending transaction from storage.
     *
     * @param transactionId Transaction hash to remove
     */
    suspend fun removeTransaction(transactionId: String) {
        mutex.withLock {
            val currentData = getPendingTransactionsData()
            val updatedTransactions = currentData.transactions.filter { it.transactionId != transactionId }

            val updatedData = currentData.copy(transactions = updatedTransactions)
            dataStorage.store(wallet.publicKey, updatedData)
        }
    }

    /**
     * Removes multiple pending transactions from storage.
     *
     * @param transactionIds List of transaction hashes to remove
     */
    suspend fun removeTransactions(transactionIds: List<String>) {
        mutex.withLock {
            val currentData = getPendingTransactionsData()
            val transactionIdsSet = transactionIds.toSet()
            val updatedTransactions = currentData.transactions.filter { it.transactionId !in transactionIdsSet }

            val updatedData = currentData.copy(transactions = updatedTransactions)
            dataStorage.store(wallet.publicKey, updatedData)
        }
    }

    /**
     * Gets current list of pending transaction IDs filtered by optional contract address.
     *
     * @param contractAddress Optional contract address to filter by
     * @return List of pending transaction IDs matching the criteria
     */
    suspend fun getTransactionIds(contractAddress: String? = null): List<String> {
        return mutex.withLock {
            val allTransactions = getPendingTransactionsData().transactions
            val result = allTransactions.filter { transaction ->
                transaction.blockchain == wallet.blockchain.id &&
                    (contractAddress == null || transaction.contractAddress == contractAddress)
            }.map { it.transactionId }
            Logger.logTransaction(
                "getPendingTransactionIds: blockchain=${wallet.blockchain.id}, contractAddress=$contractAddress, " +
                    "result=$result",
            )
            result
        }
    }

    /**
     * Gets all pending transactions for the wallet's blockchain.
     *
     * @return List of pending transactions
     */
    suspend fun getTransactions(): List<PendingTransaction> {
        return mutex.withLock {
            val result = getPendingTransactionsData().transactions.filter {
                it.blockchain == wallet.blockchain.id
            }
            result
        }
    }

    private suspend fun getPendingTransactionsData(): PendingTransactions {
        return dataStorage.getOrNull<PendingTransactions>(wallet.publicKey) ?: PendingTransactions()
    }
}