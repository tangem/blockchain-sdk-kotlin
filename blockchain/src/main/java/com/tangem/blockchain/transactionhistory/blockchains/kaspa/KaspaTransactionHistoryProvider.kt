package com.tangem.blockchain.transactionhistory.blockchains.kaspa

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.pagination.Page
import com.tangem.blockchain.common.pagination.PaginationWrapper
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryState
import com.tangem.blockchain.transactionhistory.blockchains.kaspa.network.KaspaApiService
import com.tangem.blockchain.transactionhistory.blockchains.kaspa.network.KaspaCoinTransaction
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem.DestinationType
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem.SourceType
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem.TransactionStatus.Confirmed
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem.TransactionStatus.Unconfirmed
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class KaspaTransactionHistoryProvider(
    private val blockchain: Blockchain,
    private val kaspaApiService: KaspaApiService,
) : TransactionHistoryProvider {

    override suspend fun getTransactionHistoryState(
        address: String,
        filterType: TransactionHistoryRequest.FilterType,
    ): TransactionHistoryState {
        return when (filterType) {
            TransactionHistoryRequest.FilterType.Coin -> getTxHistoryStateForCoin(address)
            is TransactionHistoryRequest.FilterType.Contract -> TransactionHistoryState.NotImplemented
        }
    }

    override suspend fun getTransactionsHistory(
        request: TransactionHistoryRequest,
    ): Result<PaginationWrapper<TransactionHistoryItem>> {
        return try {
            val pageToLoad = request.pageToLoad?.toIntOrNull() ?: 0
            val pageSize = request.pageSize
            val response = withContext(Dispatchers.IO) {
                kaspaApiService.getAddressTransactions(
                    address = request.address,
                    limit = pageSize,
                    offset = pageToLoad * pageSize,
                )
            }
            val txs = response.toTxHistoryItems(request.address)
            val nextPage = if (response.size < pageSize) Page.LastPage else Page.Next(pageToLoad.inc().toString())
            Result.Success(PaginationWrapper(nextPage = nextPage, items = txs))
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private suspend fun getTxHistoryStateForCoin(address: String): TransactionHistoryState {
        return try {
            val response = withContext(Dispatchers.IO) {
                kaspaApiService.getAddressTransactions(
                    address = address,
                    limit = 15,
                    offset = 0,
                )
            }
            if (response.isNotEmpty()) {
                TransactionHistoryState.Success.HasTransactions(response.size)
            } else {
                TransactionHistoryState.Success.Empty
            }
        } catch (e: Exception) {
            TransactionHistoryState.Failed.FetchError(e)
        }
    }

    private fun List<KaspaCoinTransaction>.toTxHistoryItems(walletAddress: String): List<TransactionHistoryItem> {
        return mapNotNull { transaction ->
            val isOutgoing = transaction.inputs.any { it.previousOutpointAddress == walletAddress }
            val amount = transaction.extractTransactionAmount(isOutgoing, walletAddress) ?: return@mapNotNull null
            val destination = transaction.extractDestination(isOutgoing, walletAddress) ?: return@mapNotNull null
            val source = transaction.extractSource(isOutgoing, walletAddress) ?: return@mapNotNull null
            TransactionHistoryItem(
                txHash = transaction.transactionId,
                timestamp = transaction.blockTime,
                isOutgoing = isOutgoing,
                destinationType = destination,
                sourceType = source,
                status = if (transaction.isAccepted) Confirmed else Unconfirmed,
                type = TransactionHistoryItem.TransactionType.Transfer,
                amount = amount,
            )
        }
    }

    private fun KaspaCoinTransaction.extractTransactionAmount(isOutgoing: Boolean, walletAddress: String): Amount? {
        val amount = if (isOutgoing) {
            outputs.firstOrNull { it.scriptPublicKeyAddress != walletAddress }?.amount
        } else {
            outputs.firstOrNull { it.scriptPublicKeyAddress == walletAddress }?.amount
        } ?: return null

        val fee = inputs.sumOf { it.previousOutpointAmount } - outputs.sumOf { it.amount }
        val amountWithFee = if (isOutgoing) amount + fee else amount

        return Amount(
            blockchain = blockchain,
            value = amountWithFee.toBigDecimal().movePointLeft(blockchain.decimals()),
        )
    }

    private fun KaspaCoinTransaction.extractDestination(isOutgoing: Boolean, walletAddress: String): DestinationType? {
        return when {
            isOutgoing -> {
                val outputAddresses = outputs
                    .filter { it.scriptPublicKeyAddress != walletAddress }
                    .map { TransactionHistoryItem.AddressType.User(it.scriptPublicKeyAddress) }

                when {
                    outputAddresses.isEmpty() -> null
                    outputAddresses.size == 1 -> DestinationType.Single(outputAddresses.first())
                    else -> DestinationType.Multiple(outputAddresses)
                }
            }
            else -> DestinationType.Single(TransactionHistoryItem.AddressType.User(walletAddress))
        }
    }

    private fun KaspaCoinTransaction.extractSource(isOutgoing: Boolean, walletAddress: String): SourceType? {
        return when {
            isOutgoing -> SourceType.Single(walletAddress)
            else ->
                inputs
                    .firstOrNull { it.previousOutpointAddress != walletAddress }
                    ?.previousOutpointAddress
                    ?.let(SourceType::Single)
        }
    }
}