package com.tangem.blockchain.transactionhistory.blockchains.polygon

import com.tangem.Log
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.pagination.Page
import com.tangem.blockchain.common.pagination.PaginationWrapper
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryState
import com.tangem.blockchain.transactionhistory.blockchains.polygon.network.*
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryRequest
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.isZero
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

@Suppress("ClassOrdering")
internal class EtherscanTransactionHistoryProvider(
    private val blockchain: Blockchain,
    private val api: EtherScanApi,
    private val etherscanApiKey: String,
) : TransactionHistoryProvider {

    @Suppress("TooGenericExceptionThrown")
    override suspend fun getTransactionHistoryState(
        address: String,
        filterType: TransactionHistoryRequest.FilterType,
    ): TransactionHistoryState {
        return try {
            val response = withContext(Dispatchers.IO) {
                when (filterType) {
                    TransactionHistoryRequest.FilterType.Coin -> api.getCoinTransactionHistory(
                        chainId = requireNotNull(blockchain.getChainId()) { "chainId must not be null" },
                        address = address,
                        page = 1,
                        offset = 1, // We don't need to know all transactions to define state
                        apiKey = etherscanApiKey,
                    )
                    is TransactionHistoryRequest.FilterType.Contract -> api.getTokenTransactionHistory(
                        chainId = requireNotNull(blockchain.getChainId()) { "chainId must not be null" },
                        address = address,
                        page = 1,
                        offset = 100, // There are might be spam transaction in first transactions
                        contractAddress = filterType.tokenInfo.contractAddress,
                        apiKey = etherscanApiKey,
                    )
                }
            }

            response.extractApiError()?.let { apiError ->
                return when (apiError) {
                    PolygonScanApiError.EndOfTransactionsReached -> TransactionHistoryState.Success.Empty
                    is PolygonScanApiError.Error -> throw Exception(apiError.message)
                }
            }

            val historyItems = response.result.toTransactionHistoryItems(
                excludeZeroAmount = false,
                walletAddress = address,
                filterType = filterType,
            )
            when {
                historyItems.isEmpty() -> TransactionHistoryState.Success.Empty
                else -> TransactionHistoryState.Success.HasTransactions(historyItems.size)
            }
        } catch (e: Exception) {
            TransactionHistoryState.Failed.FetchError(e)
        }
    }

    @Suppress("TooGenericExceptionThrown")
    override suspend fun getTransactionsHistory(
        request: TransactionHistoryRequest,
    ): Result<PaginationWrapper<TransactionHistoryItem>> {
        return try {
            val pageToLoad = request.intPageToLoad
            val response = withContext(Dispatchers.IO) {
                when (request.filterType) {
                    TransactionHistoryRequest.FilterType.Coin -> api.getCoinTransactionHistory(
                        chainId = requireNotNull(blockchain.getChainId()) { "chainId must not be null" },
                        address = request.address,
                        offset = request.pageSize,
                        page = pageToLoad,
                        apiKey = etherscanApiKey,
                    )
                    is TransactionHistoryRequest.FilterType.Contract -> api.getTokenTransactionHistory(
                        chainId = requireNotNull(blockchain.getChainId()) { "chainId must not be null" },
                        address = request.address,
                        offset = request.pageSize,
                        page = pageToLoad,
                        contractAddress = request.filterType.tokenInfo.contractAddress,
                        apiKey = etherscanApiKey,
                    )
                }
            }

            response.extractApiError()?.let { apiError ->
                return when (apiError) {
                    PolygonScanApiError.EndOfTransactionsReached -> Result.Success(
                        PaginationWrapper(nextPage = Page.LastPage, items = emptyList()),
                    )
                    is PolygonScanApiError.Error -> throw Exception(apiError.message)
                }
            }

            val txs = response.result.toTransactionHistoryItems(
                excludeZeroAmount = true,
                walletAddress = request.address,
                filterType = request.filterType,
            )
            val nextPage = if (txs.isNotEmpty()) {
                Page.Next(pageToLoad.inc().toString())
            } else {
                Page.LastPage
            }
            Result.Success(PaginationWrapper(nextPage = nextPage, items = txs))
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private fun PolygonScanResult.toTransactionHistoryItems(
        excludeZeroAmount: Boolean,
        walletAddress: String,
        filterType: TransactionHistoryRequest.FilterType,
    ): List<TransactionHistoryItem> {
        return this.transactions.orEmpty().mapNotNull { transaction ->
            val sourceAddress = transaction.from

            val transactionAmount = transaction.extractAmount(filterType = filterType).guard {
                Log.info { "Transaction with invalid value  $transaction received" }
                return@mapNotNull null
            }
            if (excludeZeroAmount && shouldExcludeFromHistory(filterType, transactionAmount)) {
                Log.info { "Transaction with zero amount is excluded from history. $transaction" }
                return@mapNotNull null
            }
            if (isLikelySpamTransaction(amount = transactionAmount, filterType = filterType)) {
                return@mapNotNull null
            }

            val isOutgoing = sourceAddress.equals(walletAddress, ignoreCase = true)

            TransactionHistoryItem(
                txHash = transaction.hash,
                timestamp = TimeUnit.SECONDS.toMillis(transaction.timeStamp.toLong()),
                isOutgoing = isOutgoing,
                destinationType = transaction.extractDestinationType(filterType = filterType, address = transaction.to),
                sourceType = TransactionHistoryItem.SourceType.Single(address = sourceAddress),
                status = transaction.extractStatus(),
                type = transaction.extractType(filterType),
                amount = transactionAmount,
            )
        }
    }

    private fun PolygonTransaction.extractDestinationType(
        filterType: TransactionHistoryRequest.FilterType,
        address: String,
    ): TransactionHistoryItem.DestinationType {
        val addressType = if (filterType is TransactionHistoryRequest.FilterType.Coin && this.isContractInteraction) {
            TransactionHistoryItem.AddressType.Contract(address = address)
        } else {
            TransactionHistoryItem.AddressType.User(address = address)
        }
        return TransactionHistoryItem.DestinationType.Single(addressType = addressType)
    }

    private fun PolygonTransaction.extractStatus(): TransactionHistoryItem.TransactionStatus {
        return when {
            isError.isBooleanTrue -> TransactionHistoryItem.TransactionStatus.Failed
            txReceiptStatus.isBooleanTrue -> TransactionHistoryItem.TransactionStatus.Confirmed
            confirmations.toInt() > 0 -> TransactionHistoryItem.TransactionStatus.Confirmed
            else -> TransactionHistoryItem.TransactionStatus.Unconfirmed
        }
    }

    private fun PolygonTransaction.extractType(
        filterType: TransactionHistoryRequest.FilterType,
    ): TransactionHistoryItem.TransactionType {
        return if (filterType is TransactionHistoryRequest.FilterType.Coin && this.isContractInteraction) {
            val fnName = this.functionName?.substringBefore("(")
            TransactionHistoryItem.TransactionType.ContractMethodName(fnName ?: this.functionName ?: UNKNOWN)
        } else {
            // All token transactions are considered simple & plain transfers
            TransactionHistoryItem.TransactionType.Transfer
        }
    }

    private fun PolygonTransaction.extractAmount(filterType: TransactionHistoryRequest.FilterType): Amount? {
        val txAmount = try {
            BigDecimal(this.value)
        } catch (_: Exception) {
            return null
        }
        return when (filterType) {
            TransactionHistoryRequest.FilterType.Coin -> Amount(
                value = txAmount.movePointLeft(blockchain.decimals()),
                blockchain = blockchain,
            )
            is TransactionHistoryRequest.FilterType.Contract -> Amount(
                value = txAmount.movePointLeft(filterType.tokenInfo.decimals),
                token = filterType.tokenInfo,
            )
        }
    }

    private val PolygonTransaction.isContractInteraction: Boolean
        get() = !contractAddress.isNullOrEmpty() || !functionName.isNullOrEmpty()

    private fun PolygonTransactionHistoryResponse.extractApiError(): PolygonScanApiError? {
        return if (!this.isSuccessful) {
            when (this.result) {
                is PolygonScanResult.Description -> PolygonScanApiError.Error(this.result.description)
                is PolygonScanResult.Transactions -> {
                    if (this.result.transactions?.isEmpty() == true) {
                        PolygonScanApiError.EndOfTransactionsReached
                    } else {
                        PolygonScanApiError.Error(this.message)
                    }
                }
            }
        } else {
            null
        }
    }

    /**
     * Zero token transfers are most likely spam transactions, for [example](https://polygonscan.com/tx/0x227a8dc404acb8659d87c75a2ac2427a1f86f802f2f9a8376dcfa2537a9abdf0).
     */
    private fun isLikelySpamTransaction(amount: Amount, filterType: TransactionHistoryRequest.FilterType): Boolean =
        filterType is TransactionHistoryRequest.FilterType.Contract && amount.value?.isZero() == true

    /**
     * API returns "1" in status field, if request were successful.
     */
    private val PolygonTransactionHistoryResponse.isSuccessful: Boolean
        get() = this.status.isBooleanTrue

    private val String?.isBooleanTrue: Boolean
        get() = this?.toIntOrNull() == 1

    private val TransactionHistoryRequest.intPageToLoad: Int
        get() = when (page) {
            Page.Initial -> 1
            is Page.Next -> page.value.toInt()
            Page.LastPage -> error("EOF reached. No data to load")
        }

    private companion object {
        const val UNKNOWN = "Unknown"
    }
}