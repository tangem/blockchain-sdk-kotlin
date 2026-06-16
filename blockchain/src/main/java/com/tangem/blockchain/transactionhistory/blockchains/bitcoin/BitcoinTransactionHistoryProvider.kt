package com.tangem.blockchain.transactionhistory.blockchains.bitcoin

import android.util.Log
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.pagination.Page
import com.tangem.blockchain.common.pagination.PaginationWrapper
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.toBigDecimalOrDefault
import com.tangem.blockchain.network.blockbook.network.BlockBookApi
import com.tangem.blockchain.network.blockbook.network.responses.GetAddressResponse
import com.tangem.blockchain.network.blockbook.network.responses.feeAmount
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryState
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem.TransactionStatus
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem.TransactionType
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

internal class BitcoinTransactionHistoryProvider(
    private val blockchain: Blockchain,
    private val blockBookApi: BlockBookApi,
) : TransactionHistoryProvider {

    override suspend fun getTransactionHistoryState(
        address: String,
        filterType: TransactionHistoryRequest.FilterType,
    ): TransactionHistoryState {
        return try {
            val response = withContext(Dispatchers.IO) {
                blockBookApi.getTransactions(
                    address = address,
                    page = null,
                    pageSize = 1, // We don't need to know all transactions to define state
                    filterType = null,
                )
            }
            if (!response.transactions.isNullOrEmpty()) {
                TransactionHistoryState.Success.HasTransactions(response.txs)
            } else {
                TransactionHistoryState.Success.Empty
            }
        } catch (e: Exception) {
            Log.e(BitcoinTransactionHistoryProvider::class.java.simpleName, e.message, e)
            TransactionHistoryState.Failed.FetchError(e)
        }
    }

    override suspend fun getTransactionsHistory(
        request: TransactionHistoryRequest,
    ): Result<PaginationWrapper<TransactionHistoryItem>> {
        return try {
            val response =
                withContext(Dispatchers.IO) {
                    blockBookApi.getTransactions(
                        address = request.address,
                        page = request.pageToLoad,
                        pageSize = request.pageSize,
                        filterType = null,
                    )
                }
            val txs = response.transactions
                ?.mapNotNull { tx -> tx.toTransactionHistoryItem(request.address) }
                ?: emptyList()
            val nextPage = nextPage(
                currentPage = response.page,
                totalPages = response.totalPages,
                requestedPage = request.page,
            )
            Result.Success(
                PaginationWrapper(
                    nextPage = nextPage,
                    items = txs,
                ),
            )
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    /**
     * Dynamic-addresses (XPUB) variant of [getTransactionHistoryState].
     * Queries BlockBook by [descriptor] (e.g. `wpkh(xpub)` / `pkh(xpub)`) instead of a single address.
     */
    suspend fun getTransactionHistoryStateByXpub(descriptor: String): TransactionHistoryState {
        return try {
            val response = withContext(Dispatchers.IO) {
                // We don't need to know all transactions to define state
                blockBookApi.getXpubInfo(descriptor = descriptor, page = null, pageSize = 1)
            }
            if (!response.transactions.isNullOrEmpty()) {
                TransactionHistoryState.Success.HasTransactions(response.txs)
            } else {
                TransactionHistoryState.Success.Empty
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(BitcoinTransactionHistoryProvider::class.java.simpleName, e.message, e)
            TransactionHistoryState.Failed.FetchError(e)
        }
    }

    /**
     * Dynamic-addresses (XPUB) variant of [getTransactionsHistory].
     * Direction/amount/source/destination are resolved via the `isOwn` flag that BlockBook sets on
     * inputs/outputs belonging to the queried descriptor, so all derived addresses (and internal
     * transfers between them) are handled correctly — unlike the single-address path.
     */
    suspend fun getTransactionsHistoryByXpub(
        descriptor: String,
        request: TransactionHistoryRequest,
    ): Result<PaginationWrapper<TransactionHistoryItem>> {
        return try {
            val response = withContext(Dispatchers.IO) {
                blockBookApi.getXpubInfo(
                    descriptor = descriptor,
                    page = request.pageToLoad?.toIntOrNull(),
                    pageSize = request.pageSize,
                )
            }
            val txs = response.transactions
                ?.mapNotNull { tx -> tx.toTransactionHistoryItemByOwnership() }
                .orEmpty()
            val nextPage = nextPage(
                currentPage = response.page,
                totalPages = response.totalPages,
                requestedPage = request.page,
            )
            Result.Success(
                PaginationWrapper(
                    nextPage = nextPage,
                    items = txs,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private fun nextPage(currentPage: Int?, totalPages: Int?, requestedPage: Page): Page {
        if (currentPage == null || requestedPage is Page.LastPage) return Page.LastPage
        if (totalPages == null || currentPage >= totalPages) return Page.LastPage
        return Page.Next(currentPage.inc().toString())
    }

    private fun GetAddressResponse.Transaction.toTransactionHistoryItem(
        walletAddress: String,
    ): TransactionHistoryItem? {
        val isOutgoing = vin.any { it.addresses?.contains(walletAddress) == true }
        val transactionAmount = extractAmount(
            isOutgoing = isOutgoing,
            tx = this,
            walletAddress = walletAddress,
            blockchain = blockchain,
        )

        return TransactionHistoryItem(
            txHash = txid,
            timestamp = TimeUnit.SECONDS.toMillis(blockTime.toLong()),
            isOutgoing = isOutgoing,
            destinationType = extractDestinationType(tx = this, walletAddress = walletAddress),
            sourceType = sourceType(tx = this, walletAddress = walletAddress),
            status = if (confirmations > 0) TransactionStatus.Confirmed else TransactionStatus.Unconfirmed,
            type = TransactionType.Transfer,
            amount = transactionAmount,
            fee = this.feeAmount(blockchain),
        )
    }

    private fun extractDestinationType(
        tx: GetAddressResponse.Transaction,
        walletAddress: String,
    ): TransactionHistoryItem.DestinationType {
        val outputsWithOtherAddresses = tx.vout
            .filter { it.addresses?.contains(walletAddress) == false }
            .mapNotNull { it.addresses }
            .flatten()
            .toSet()
        return when {
            outputsWithOtherAddresses.isEmpty() -> TransactionHistoryItem.DestinationType.Single(
                TransactionHistoryItem.AddressType.User(walletAddress),
            )

            outputsWithOtherAddresses.size == 1 -> TransactionHistoryItem.DestinationType.Single(
                TransactionHistoryItem.AddressType.User(outputsWithOtherAddresses.first()),
            )

            else -> TransactionHistoryItem.DestinationType.Multiple(
                outputsWithOtherAddresses.map { TransactionHistoryItem.AddressType.User(it) },
            )
        }
    }

    private fun sourceType(
        tx: GetAddressResponse.Transaction,
        walletAddress: String,
    ): TransactionHistoryItem.SourceType {
        val inputsWithOtherAddresses = tx.vin
            .filter { it.addresses?.contains(walletAddress) == false }
            .mapNotNull { it.addresses }
            .flatten()
            .toSet()
        return when {
            inputsWithOtherAddresses.isEmpty() -> TransactionHistoryItem.SourceType.Single(walletAddress)
            inputsWithOtherAddresses.size == 1 -> TransactionHistoryItem.SourceType.Single(
                inputsWithOtherAddresses.first(),
            )

            else -> TransactionHistoryItem.SourceType.Multiple(inputsWithOtherAddresses.toList())
        }
    }

    private fun extractAmount(
        isOutgoing: Boolean,
        tx: GetAddressResponse.Transaction,
        walletAddress: String,
        blockchain: Blockchain,
    ): Amount {
        return try {
            val amount = if (isOutgoing) {
                val outputs = tx.vout
                    .filter { it.addresses?.contains(walletAddress) == false }
                    .mapNotNull { it.value?.toBigDecimalOrNull() }
                    .sumOf { it }
                val fee = tx.fees.toBigDecimalOrDefault()
                outputs + fee
            } else {
                val outputs = tx.vout
                    .filter { it.addresses?.contains(walletAddress) == true }
                    .map { it.value.toBigDecimalOrDefault() }
                    .sumOf { it }
                val inputs = tx.vin
                    .filter { it.addresses?.contains(walletAddress) == true }
                    .map { it.value.toBigDecimalOrDefault() }
                    .sumOf { it }
                outputs - inputs
            }

            Amount(value = amount.movePointLeft(blockchain.decimals()), blockchain = blockchain)
        } catch (e: Exception) {
            Amount(blockchain = blockchain)
        }
    }

    private fun GetAddressResponse.Transaction.toTransactionHistoryItemByOwnership(): TransactionHistoryItem? {
        val isOutgoing = vin.any { it.isOwn == true }
        val transactionAmount = extractAmountByOwnership(isOutgoing = isOutgoing, tx = this, blockchain = blockchain)

        return TransactionHistoryItem(
            txHash = txid,
            timestamp = TimeUnit.SECONDS.toMillis(blockTime.toLong()),
            isOutgoing = isOutgoing,
            destinationType = extractDestinationTypeByOwnership(tx = this),
            sourceType = sourceTypeByOwnership(tx = this),
            status = if (confirmations > 0) TransactionStatus.Confirmed else TransactionStatus.Unconfirmed,
            type = TransactionType.Transfer,
            amount = transactionAmount,
            fee = this.feeAmount(blockchain),
        )
    }

    private fun extractDestinationTypeByOwnership(
        tx: GetAddressResponse.Transaction,
    ): TransactionHistoryItem.DestinationType {
        val externalOutputs = tx.vout
            .filter { it.isOwn != true }
            .mapNotNull { it.addresses }
            .flatten()
            .toSet()
        return when {
            externalOutputs.isEmpty() -> TransactionHistoryItem.DestinationType.Single(
                TransactionHistoryItem.AddressType.User(tx.firstOwnVoutAddress().orEmpty()),
            )

            externalOutputs.size == 1 -> TransactionHistoryItem.DestinationType.Single(
                TransactionHistoryItem.AddressType.User(externalOutputs.first()),
            )

            else -> TransactionHistoryItem.DestinationType.Multiple(
                externalOutputs.map { TransactionHistoryItem.AddressType.User(it) },
            )
        }
    }

    private fun sourceTypeByOwnership(tx: GetAddressResponse.Transaction): TransactionHistoryItem.SourceType {
        val externalInputs = tx.vin
            .filter { it.isOwn != true }
            .mapNotNull { it.addresses }
            .flatten()
            .toSet()
        return when {
            externalInputs.isEmpty() -> TransactionHistoryItem.SourceType.Single(tx.firstOwnVinAddress().orEmpty())
            externalInputs.size == 1 -> TransactionHistoryItem.SourceType.Single(externalInputs.first())
            else -> TransactionHistoryItem.SourceType.Multiple(externalInputs.toList())
        }
    }

    private fun extractAmountByOwnership(
        isOutgoing: Boolean,
        tx: GetAddressResponse.Transaction,
        blockchain: Blockchain,
    ): Amount {
        return try {
            val amount = if (isOutgoing) {
                val outputs = tx.vout
                    .filter { it.isOwn != true }
                    .mapNotNull { it.value?.toBigDecimalOrNull() }
                    .sumOf { it }
                outputs + tx.fees.toBigDecimalOrDefault()
            } else {
                tx.vout
                    .filter { it.isOwn == true }
                    .mapNotNull { it.value?.toBigDecimalOrNull() }
                    .sumOf { it }
            }

            Amount(value = amount.movePointLeft(blockchain.decimals()), blockchain = blockchain)
        } catch (e: Exception) {
            Amount(blockchain = blockchain)
        }
    }

    private fun GetAddressResponse.Transaction.firstOwnVoutAddress(): String? =
        vout.firstOrNull { it.isOwn == true }?.addresses?.firstOrNull()

    private fun GetAddressResponse.Transaction.firstOwnVinAddress(): String? =
        vin.firstOrNull { it.isOwn == true }?.addresses?.firstOrNull()
}