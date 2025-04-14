package com.tangem.blockchain.transactionhistory.blockchains.algorand

import com.tangem.blockchain.transactionhistory.blockchains.algorand.network.AlgorandIndexerApi
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.pagination.Page
import com.tangem.blockchain.common.pagination.PaginationWrapper
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryRequest
import com.tangem.blockchain.transactionhistory.TransactionHistoryState
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.transactionhistory.blockchains.algorand.network.AlgorandPaymentTransaction
import com.tangem.blockchain.transactionhistory.blockchains.algorand.network.AlgorandTransactionHistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class AlgorandTransactionHistoryProvider(
    private val blockchain: Blockchain,
    private val algorandIndexerApi: AlgorandIndexerApi,
) : TransactionHistoryProvider {

    override suspend fun getTransactionHistoryState(
        address: String,
        filterType: TransactionHistoryRequest.FilterType,
    ): TransactionHistoryState {
        return try {
            val response = withContext(Dispatchers.IO) {
                algorandIndexerApi.getTransactions(
                    address = address,
                    next = null,
                    limit = 1, // We don't need to know all transactions to define state
                )
            }
            if (response.transactions.isNotEmpty()) {
                TransactionHistoryState.Success.HasTransactions(response.transactions.size)
            } else {
                TransactionHistoryState.Success.Empty
            }
        } catch (e: Exception) {
            TransactionHistoryState.Failed.FetchError(e)
        }
    }

    override suspend fun getTransactionsHistory(
        request: TransactionHistoryRequest,
    ): Result<PaginationWrapper<TransactionHistoryItem>> {
        return try {
            val response = withContext(Dispatchers.IO) {
                algorandIndexerApi.getTransactions(
                    address = request.address,
                    limit = request.pageSize,
                    next = request.pageToLoad,
                )
            }
            val txs = response.transactions
                .mapNotNull { tx -> tx.toTransactionHistoryItem(request.address) }
            val nextPage = if (response.transactions.isNotEmpty() && response.nextToken != null) {
                Page.Next(response.nextToken)
            } else {
                Page.LastPage
            }
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

    private fun AlgorandTransactionHistoryItem.toTransactionHistoryItem(
        walletAddress: String,
    ): TransactionHistoryItem? {
        if (this.paymentTransaction == null) return null

        val isOutgoing = walletAddress.equals(sender, ignoreCase = true)
        val transactionAmount = this.extractAmount(isOutgoing, paymentTransaction)

        return TransactionHistoryItem(
            txHash = this.id,
            timestamp = this.roundTime?.times(other = 1000) ?: 0L,
            isOutgoing = isOutgoing,
            destinationType = TransactionHistoryItem.DestinationType.Single(
                addressType = TransactionHistoryItem.AddressType.User(this.paymentTransaction.receiver),
            ),
            sourceType = TransactionHistoryItem.SourceType.Single(address = this.sender),
            status = this.extractStatus(),
            type = TransactionHistoryItem.TransactionType.Transfer,
            amount = transactionAmount,
        )
    }

    private fun AlgorandTransactionHistoryItem.extractStatus(): TransactionHistoryItem.TransactionStatus =
        if (confirmedRound != null) {
            TransactionHistoryItem.TransactionStatus.Confirmed
        } else {
            TransactionHistoryItem.TransactionStatus.Unconfirmed
        }

    private fun AlgorandTransactionHistoryItem.extractAmount(
        isOutgoing: Boolean,
        paymentTransaction: AlgorandPaymentTransaction,
    ): Amount {
        val txAmount = paymentTransaction.amount.toBigDecimal().movePointLeft(blockchain.decimals())
        val feeAmount = this.fee.toBigDecimal().movePointLeft(blockchain.decimals())
        return Amount(blockchain = blockchain, value = if (isOutgoing) txAmount.plus(feeAmount) else txAmount)
    }
}