package com.tangem.blockchain.blockchains.tron

import com.tangem.Log
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.txhistory.TransactionHistoryItem
import com.tangem.blockchain.common.txhistory.TransactionHistoryItem.TransactionStatus
import com.tangem.blockchain.common.txhistory.TransactionHistoryProvider
import com.tangem.blockchain.common.txhistory.TransactionHistoryRequest
import com.tangem.blockchain.common.txhistory.TransactionHistoryState
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.blockbook.network.BlockBookApi
import com.tangem.blockchain.network.blockbook.network.responses.GetAddressResponse
import com.tangem.common.extensions.guard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

private const val PREFIX = "0x"

internal class TronTransactionHistoryProvider(
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
                    page = 1,
                    pageSize = 1, // We don't need to know all transactions to define state
                    filterType = filterType,
                )
            }
            if (!response.transactions.isNullOrEmpty()) {
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
            val response =
                withContext(Dispatchers.IO) {
                    blockBookApi.getTransactions(
                        address = request.address,
                        page = request.page.number,
                        pageSize = request.page.size,
                        filterType = request.filterType,
                    )
                }
            val txs = response.transactions
                ?.mapNotNull { tx ->
                    tx.toTransactionHistoryItem(
                        walletAddress = request.address,
                        decimals = request.decimals,
                        filterType = request.filterType,
                    )
                }
                ?: emptyList()
            Result.Success(
                PaginationWrapper(
                    page = response.page ?: request.page.number,
                    totalPages = response.totalPages ?: 0,
                    itemsOnPage = response.itemsOnPage ?: 0,
                    items = txs,
                ),
            )
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private fun GetAddressResponse.Transaction.toTransactionHistoryItem(
        walletAddress: String,
        decimals: Int,
        filterType: TransactionHistoryRequest.FilterType,
    ): TransactionHistoryItem? {
        val destinationType = extractDestinationType(this, filterType).guard {
            Log.info { "Transaction $this doesn't contain a required value" }
            return null
        }
        val amount = extractAmount(tx = this, decimals = decimals, filterType = filterType).guard {
            Log.info { "Transaction $this doesn't contain a required value" }
            return null
        }
        val sourceType = extractSourceType(tx = this, filterType = filterType).guard {
            Log.info { "Transaction $this doesn't contain a required value" }
            return null
        }
        return TransactionHistoryItem(
            txHash = txid.removePrefix(PREFIX),
            timestamp = TimeUnit.SECONDS.toMillis(blockTime.toLong()),
            isOutgoing = fromAddress?.equals(walletAddress, ignoreCase = true) == true,
            destinationType = destinationType,
            sourceType = sourceType,
            status = if (confirmations > 0) TransactionStatus.Confirmed else TransactionStatus.Unconfirmed,
            type = extractType(tx = this),
            amount = amount,
        )
    }

    private fun extractDestinationType(
        tx: GetAddressResponse.Transaction,
        filterType: TransactionHistoryRequest.FilterType,
    ): TransactionHistoryItem.DestinationType? {
        tx.toAddress ?: return null
        tx.fromAddress ?: return null
        return when (filterType) {
            TransactionHistoryRequest.FilterType.Coin -> {
                TransactionHistoryItem.DestinationType.Single(
                    addressType = if (tx.tokenTransfers.isEmpty()) {
                        TransactionHistoryItem.AddressType.User(tx.toAddress)
                    } else {
                        TransactionHistoryItem.AddressType.Contract(tx.toAddress)
                    },
                )
            }

            is TransactionHistoryRequest.FilterType.Contract -> {
                val transfer = tx.getTokenTransfer(filterType.address) ?: return null
                TransactionHistoryItem.DestinationType.Single(
                    addressType = TransactionHistoryItem.AddressType.User(transfer.to),
                )
            }
        }
    }

    private fun extractSourceType(
        tx: GetAddressResponse.Transaction,
        filterType: TransactionHistoryRequest.FilterType,
    ): TransactionHistoryItem.SourceType? {
        val address = when (filterType) {
            TransactionHistoryRequest.FilterType.Coin -> tx.fromAddress
            is TransactionHistoryRequest.FilterType.Contract -> {
                tx.getTokenTransfer(filterType.address)?.from
            }
        }.guard { return null }

        return TransactionHistoryItem.SourceType.Single(address = address)
    }

    private fun extractType(tx: GetAddressResponse.Transaction): TransactionHistoryItem.TransactionType {
        // Contract type 1 means transfer
        if (tx.contractType == 1) return TransactionHistoryItem.TransactionType.Transfer

        return TransactionHistoryItem.TransactionType.ContractMethod(id = tx.contractAddress.orEmpty())
    }

    private fun extractAmount(
        tx: GetAddressResponse.Transaction,
        decimals: Int,
        filterType: TransactionHistoryRequest.FilterType,
    ): Amount? {
        return when (filterType) {
            TransactionHistoryRequest.FilterType.Coin -> Amount(
                value = BigDecimal(tx.value).movePointLeft(blockchain.decimals()),
                blockchain = blockchain,
                type = AmountType.Coin,
            )

            is TransactionHistoryRequest.FilterType.Contract -> {
                val transfer = tx.getTokenTransfer(filterType.address) ?: return null
                val transferValue = transfer.value ?: "0"
                val token = Token(
                    name = transfer.name.orEmpty(),
                    symbol = transfer.symbol.orEmpty(),
                    contractAddress = transfer.token.orEmpty(),
                    decimals = decimals,
                )
                Amount(value = BigDecimal(transferValue).movePointLeft(decimals), token = token)
            }
        }
    }

    private fun GetAddressResponse.Transaction.getTokenTransfer(
        contractAddress: String,
    ): GetAddressResponse.Transaction.TokenTransfer? {
        return tokenTransfers.firstOrNull { contractAddress.equals(it.token, ignoreCase = true) }
    }
}
