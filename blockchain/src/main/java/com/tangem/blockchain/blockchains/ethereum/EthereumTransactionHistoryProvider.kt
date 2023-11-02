package com.tangem.blockchain.blockchains.ethereum

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
import com.tangem.blockchain.network.blockbook.network.responses.GetAddressResponse.Transaction.EthereumSpecific
import com.tangem.common.extensions.guard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

internal class EthereumTransactionHistoryProvider(
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

    override suspend fun getTransactionsHistory(request: TransactionHistoryRequest): Result<PaginationWrapper<TransactionHistoryItem>> {
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
                        filterType = request.filterType
                    )
                }
                ?: emptyList()
            Result.Success(
                PaginationWrapper(
                    page = response.page ?: request.page.number,
                    totalPages = response.totalPages ?: 0,
                    itemsOnPage = response.itemsOnPage ?: 0,
                    items = txs
                )
            )
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private fun GetAddressResponse.Transaction.toTransactionHistoryItem(
        walletAddress: String,
        filterType: TransactionHistoryRequest.FilterType,
    ): TransactionHistoryItem? {
        val destinationType = extractDestinationType(walletAddress, this, filterType).guard {
            Log.info { "Transaction $this doesn't contain a required value" }
            return null
        }
        val sourceType = extractSourceType(this).guard {
            Log.info { "Transaction $this doesn't contain a required value" }
            return null
        }
        val amount = extractAmount(tx = this, filterType = filterType).guard {
            Log.info { "Transaction $this doesn't contain a required value" }
            return null
        }

        return TransactionHistoryItem(
            txHash = txid,
            timestamp = TimeUnit.SECONDS.toMillis(blockTime.toLong()),
            isOutgoing = isOutgoing(walletAddress, this, filterType),
            destinationType = destinationType,
            sourceType = sourceType,
            status = extractStatus(tx = this),
            type = extractType(tx = this),
            amount = amount,
        )
    }

    private fun extractStatus(tx: GetAddressResponse.Transaction): TransactionStatus {
        val status = tx.ethereumSpecific?.status.guard {
            return if (tx.confirmations > 0) TransactionStatus.Confirmed else TransactionStatus.Unconfirmed
        }

        return when (EthereumSpecific.StatusType.fromType(status)) {
            EthereumSpecific.StatusType.PENDING -> TransactionStatus.Unconfirmed
            EthereumSpecific.StatusType.FAILURE -> TransactionStatus.Failed
            EthereumSpecific.StatusType.OK -> TransactionStatus.Confirmed
        }
    }

    private fun extractType(tx: GetAddressResponse.Transaction): TransactionHistoryItem.TransactionType {
        val methodId = tx.ethereumSpecific?.parsedData?.methodId.guard {
            return TransactionHistoryItem.TransactionType.Transfer
        }

        // MethodId is empty for the coin transfers
        if (methodId.isEmpty()) return TransactionHistoryItem.TransactionType.Transfer

        return TransactionHistoryItem.TransactionType.ContractMethod(id = methodId)
    }

    private fun isOutgoing(
        walletAddress: String,
        transaction: GetAddressResponse.Transaction,
        filterType: TransactionHistoryRequest.FilterType,
    ): Boolean {
        return when (filterType) {
            TransactionHistoryRequest.FilterType.Coin -> transaction.vin
                .firstOrNull()
                ?.addresses
                ?.firstOrNull()
                .equals(walletAddress, ignoreCase = true)

            is TransactionHistoryRequest.FilterType.Contract -> transaction.tokenTransfers
                .firstOrNull { filterType.address.equals(it.contract, true) }
                ?.from.equals(walletAddress, ignoreCase = true)
        }
    }

    private fun extractDestinationType(
        walletAddress: String,
        tx: GetAddressResponse.Transaction,
        filterType: TransactionHistoryRequest.FilterType,
    ): TransactionHistoryItem.DestinationType? {
        val address = tx.vout
            .firstOrNull()
            ?.addresses
            ?.firstOrNull()
            .guard { return null }

        return when (filterType) {
            TransactionHistoryRequest.FilterType.Coin -> {
                TransactionHistoryItem.DestinationType.Single(
                    addressType = if (tx.tokenTransfers.isEmpty()) {
                        TransactionHistoryItem.AddressType.User(address)
                    } else {
                        TransactionHistoryItem.AddressType.Contract(address)
                    }
                )
            }

            is TransactionHistoryRequest.FilterType.Contract -> {
                val transfer = tx.tokenTransfers
                    .firstOrNull { filterType.address.equals(it.contract, ignoreCase = true) }
                    .guard { return null }
                val isOutgoing = transfer.from == walletAddress
                TransactionHistoryItem.DestinationType.Single(
                    addressType = if (isOutgoing) {
                        TransactionHistoryItem.AddressType.User(transfer.to)
                    } else {
                        TransactionHistoryItem.AddressType.User(transfer.from)
                    },
                )
            }
        }
    }

    private fun extractSourceType(tx: GetAddressResponse.Transaction): TransactionHistoryItem.SourceType? {
        val address = tx.vin
            .firstOrNull()
            ?.addresses
            ?.firstOrNull()
            .guard { return null }
        return TransactionHistoryItem.SourceType.Single(address = address)
    }

    private fun extractAmount(
        tx: GetAddressResponse.Transaction,
        filterType: TransactionHistoryRequest.FilterType,
    ): Amount? {
        return when (filterType) {
            TransactionHistoryRequest.FilterType.Coin -> Amount(
                value = BigDecimal(tx.value).movePointLeft(blockchain.decimals()),
                blockchain = blockchain,
                type = AmountType.Coin
            )

            is TransactionHistoryRequest.FilterType.Contract -> {
                val transfer = tx.tokenTransfers
                    .firstOrNull { filterType.address.equals(it.contract, ignoreCase = true) }
                    .guard { return null }
                val transferValue = transfer.value ?: "0"
                val token = Token(
                    name = transfer.name.orEmpty(),
                    symbol = transfer.symbol.orEmpty(),
                    contractAddress = transfer.contract,
                    decimals = transfer.decimals,
                )
                Amount(value = BigDecimal(transferValue).movePointLeft(transfer.decimals), token = token)
            }
        }
    }
}
