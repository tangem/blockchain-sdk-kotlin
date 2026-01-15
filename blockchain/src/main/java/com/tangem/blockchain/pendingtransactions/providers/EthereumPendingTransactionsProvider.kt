package com.tangem.blockchain.pendingtransactions.providers

import com.tangem.blockchain.common.logging.Logger
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.blockchains.ethereum.network.EthereumTransactionResponse
import com.tangem.blockchain.common.JsonRPCResponse
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.datastorage.PendingTransaction
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.map
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.network.moshi
import com.tangem.blockchain.pendingtransactions.PendingTransactionStatus
import com.tangem.blockchain.pendingtransactions.PendingTransactionStorage
import com.tangem.blockchain.pendingtransactions.PendingTransactionsProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Ethereum implementation of [PendingTransactionsProvider].
 * Manages pending transactions storage and checking for EVM-compatible blockchains.
 *
 * @property wallet Wallet instance
 * @property multiJsonRpcProvider Multi-provider for JSON-RPC calls
 * @property storage Storage for pending transactions
 * @property networkProviderMap Map of provider types to network providers
 */
internal class EthereumPendingTransactionsProvider(
    private val wallet: Wallet,
    private val multiJsonRpcProvider: MultiNetworkProvider<EthereumJsonRpcProvider>,
    private val storage: PendingTransactionStorage,
    private val networkProviderMap: Map<ProviderType, NetworkProvider>,
) : PendingTransactionsProvider {

    private val transactionResponseAdapter by lazy {
        moshi.adapter(EthereumTransactionResponse::class.java)
    }
    private val networkProviderMapper = NetworkProviderMapper()

    override suspend fun addPendingTransaction(
        transactionId: String,
        networkProvider: NetworkProvider,
        contractAddress: String?,
    ) {
        Logger.logTransaction("$LOG_TAG addPendingTransaction: txId=$transactionId, contractAddress=$contractAddress")
        val providerType = networkProviderMap.entries.firstOrNull {
            it.value.baseUrl == networkProvider.baseUrl
        }?.key
        val providerName = providerType?.let { networkProviderMapper.toStorageKey(it) }
        Logger.logTransaction("$LOG_TAG addPendingTransaction: providerName=$providerName")
        storage.addTransaction(transactionId, providerName, contractAddress)
    }

    override suspend fun removePendingTransaction(transactionId: String) {
        Logger.logTransaction("$LOG_TAG removePendingTransaction: txId=$transactionId")
        storage.removeTransaction(transactionId)
    }

    override suspend fun getPendingTransactions(contractAddress: String?): List<String> {
        val transactions = storage.getTransactionIds(contractAddress)
        Logger.logTransaction(
            "$LOG_TAG getPendingTransactions: contractAddress=$contractAddress found ${transactions.size} transactions",
        )
        return transactions
    }

    override suspend fun checkPendingTransactions(): Map<String, PendingTransactionStatus> {
        val pendingTransactions = storage.getTransactions()
        Logger.logTransaction(
            "$LOG_TAG checkPendingTransactions: found ${pendingTransactions.size} pending transactions",
        )

        if (pendingTransactions.isEmpty()) {
            return emptyMap()
        }

        val statusMap = coroutineScope {
            pendingTransactions.map { pendingTx ->
                async {
                    val status = checkTransactionStatus(pendingTx)
                    Logger.logTransaction(
                        "$LOG_TAG checkPendingTransactions: tx ${pendingTx.transactionId} status = $status",
                    )
                    pendingTx.transactionId to status
                }
            }.awaitAll().toMap()
        }

        val transactionsToRemove = statusMap.filterValues { status ->
            status is PendingTransactionStatus.Dropped || status is PendingTransactionStatus.Executed
        }.keys.toList()

        if (transactionsToRemove.isNotEmpty()) {
            storage.removeTransactions(transactionsToRemove)
        }

        return statusMap
    }

    private suspend fun checkTransactionStatus(pendingTransaction: PendingTransaction): PendingTransactionStatus {
        Logger.logTransaction("$LOG_TAG checkTransactionStatus: txId=${pendingTransaction.transactionId}")
        val specificProvider = getProviderForTransaction(pendingTransaction)

        return checkTransactionStatusWithProvider(pendingTransaction, specificProvider)
    }

    private suspend fun checkTransactionStatusWithProvider(
        pendingTransaction: PendingTransaction,
        provider: MultiNetworkProvider<EthereumJsonRpcProvider>,
    ): PendingTransactionStatus {
        Logger.logTransaction(
            "$LOG_TAG checkTransactionStatusWithProvider: txId=${pendingTransaction.transactionId}",
        )
        return when (val result = getTransactionByHash(pendingTransaction.transactionId, provider)) {
            is Result.Success -> {
                val response = result.data
                when {
                    response == null -> PendingTransactionStatus.Dropped
                    response.isExecuted() -> PendingTransactionStatus.Executed
                    else -> PendingTransactionStatus.Pending
                }
            }
            is Result.Failure -> {
                handleFailureResult(pendingTransaction, provider)
            }
        }
    }

    private suspend fun handleFailureResult(
        pendingTransaction: PendingTransaction,
        usedProvider: MultiNetworkProvider<EthereumJsonRpcProvider>,
    ): PendingTransactionStatus {
        val isPrivateProvider = pendingTransaction.providerName != null &&
            networkProviderMapper.isPrivateProvider(pendingTransaction.providerName)

        if (isPrivateProvider && usedProvider !== multiJsonRpcProvider) {
            Logger.logTransaction(
                "$LOG_TAG handleFailureResult: specific provider failed, " +
                    "trying fallback for tx=${pendingTransaction.transactionId}",
            )
            return checkTransactionStatusWithFallback(pendingTransaction)
        }

        if (isPrivateProvider) {
            val timeSinceSent = System.currentTimeMillis() - pendingTransaction.sentAt
            return if (timeSinceSent > PRIVATE_MEMPOOL_TIMEOUT_MS) {
                Logger.logTransaction(
                    "$LOG_TAG handleFailureResult: timeout exceeded for tx=${pendingTransaction.transactionId}",
                )
                PendingTransactionStatus.Dropped
            } else {
                Logger.logTransaction(
                    "$LOG_TAG handleFailureResult: still within timeout for tx=${pendingTransaction.transactionId}",
                )
                PendingTransactionStatus.Pending
            }
        }

        return PendingTransactionStatus.Pending
    }

    private suspend fun checkTransactionStatusWithFallback(
        pendingTransaction: PendingTransaction,
    ): PendingTransactionStatus {
        Logger.logTransaction(
            "$LOG_TAG checkTransactionStatusWithFallback: txId=${pendingTransaction.transactionId}",
        )
        return when (val result = getTransactionByHash(pendingTransaction.transactionId, multiJsonRpcProvider)) {
            is Result.Success -> {
                val response = result.data
                when {
                    response != null && response.isExecuted() -> {
                        Logger.logTransaction(
                            "$LOG_TAG checkTransactionStatusWithFallback: tx executed (found via fallback)",
                        )
                        PendingTransactionStatus.Executed
                    }
                    response != null && response.isPending() -> {
                        Logger.logTransaction(
                            "$LOG_TAG checkTransactionStatusWithFallback: tx pending (found via fallback)",
                        )
                        PendingTransactionStatus.Pending
                    }
                    else -> {
                        Logger.logTransaction(
                            "$LOG_TAG checkTransactionStatusWithFallback: tx not found, using timer logic",
                        )
                        getStatusByTimer(pendingTransaction)
                    }
                }
            }
            is Result.Failure -> {
                Logger.logTransaction(
                    "$LOG_TAG checkTransactionStatusWithFallback: fallback also failed, using timer logic",
                )
                getStatusByTimer(pendingTransaction)
            }
        }
    }

    private fun getStatusByTimer(pendingTransaction: PendingTransaction): PendingTransactionStatus {
        val timeSinceSent = System.currentTimeMillis() - pendingTransaction.sentAt
        return if (timeSinceSent > PRIVATE_MEMPOOL_TIMEOUT_MS) {
            Logger.logTransaction(
                "$LOG_TAG getStatusByTimer: timeout exceeded for tx=${pendingTransaction.transactionId}",
            )
            PendingTransactionStatus.Dropped
        } else {
            Logger.logTransaction(
                "$LOG_TAG getStatusByTimer: still within timeout for tx=${pendingTransaction.transactionId}",
            )
            PendingTransactionStatus.Pending
        }
    }

    private suspend fun getTransactionByHash(
        transactionHash: String,
        multiNetworkProvider: MultiNetworkProvider<EthereumJsonRpcProvider>,
    ): Result<EthereumTransactionResponse?> {
        Logger.logTransaction("$LOG_TAG getTransactionByHash: hash=$transactionHash")
        return try {
            val response = multiNetworkProvider.performRequest(
                request = EthereumJsonRpcProvider::getTransactionByHash,
                data = transactionHash,
            )

            response.map { jsonRpcResponse: JsonRPCResponse ->
                val result = jsonRpcResponse.result
                when {
                    result == null -> null
                    else -> runCatching {
                        transactionResponseAdapter.fromJsonValue(result)
                    }.getOrNull()
                }
            }
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private fun getProviderForTransaction(
        pendingTransaction: PendingTransaction,
    ): MultiNetworkProvider<EthereumJsonRpcProvider> {
        Logger.logTransaction(
            "$LOG_TAG getProviderForTransaction: txId=${pendingTransaction.transactionId}," +
                " providerName=${pendingTransaction.providerName}",
        )
        val providerType = pendingTransaction.providerName?.let {
            networkProviderMapper.findProviderTypeByStorageKey(it, networkProviderMap.keys)
        }
        Logger.logTransaction("$LOG_TAG getProviderForTransaction: resolved providerType=$providerType")
        return if (providerType != null && networkProviderMap[providerType] != null) {
            val provider = networkProviderMap[providerType] as EthereumJsonRpcProvider
            Logger.logTransaction("$LOG_TAG getProviderForTransaction: using specific provider for type=$providerType")
            MultiNetworkProvider(
                providers = listOf(provider),
                blockchain = wallet.blockchain,
            )
        } else {
            Logger.logTransaction("$LOG_TAG getProviderForTransaction: using multiJsonRpcProvider (fallback)")
            multiJsonRpcProvider
        }
    }

    companion object {
        private const val PRIVATE_MEMPOOL_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes
        internal const val LOG_TAG = "PendingTransactionsProvider"
    }
}