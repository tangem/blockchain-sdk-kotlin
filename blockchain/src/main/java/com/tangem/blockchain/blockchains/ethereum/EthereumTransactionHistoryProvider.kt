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

    override suspend fun getTransactionHistoryState(address: String): TransactionHistoryState {
        return try {
            val addressResponse = withContext(Dispatchers.IO) { blockBookApi.getAddress(address) }
            if (!addressResponse.transactions.isNullOrEmpty()) {
                TransactionHistoryState.Success.HasTransactions(addressResponse.transactions.size)
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
                    page = response.page,
                    totalPages = response.totalPages,
                    itemsOnPage = response.itemsOnPage,
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
        val isIncoming = checkIsIncoming(walletAddress, this, filterType)
        val amount = extractAmount(tx = this, filterType = filterType).guard {
            Log.info { "Transaction $this doesn't contain a required value" }
            return null
        }

        return TransactionHistoryItem(
            txHash = txid,
            timestamp = TimeUnit.SECONDS.toMillis(blockTime.toLong()),
            direction = extractTransactionDirection(
                isIncoming = isIncoming,
                tx = this,
            ),
            status = extractStatus(tx = this),
            type = extractType(tx = this),
            amount = amount,
        )
    }

    private fun checkIsIncoming(
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
                .not()
            is TransactionHistoryRequest.FilterType.Contract -> {
                val transfer = transaction.tokenTransfers.firstOrNull { filterType.address.equals(it.contract, true) }
                return !transfer?.from.equals(walletAddress, ignoreCase = true)
            }
        }
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

        return when (methodId) {
            "0xa9059cbb" -> TransactionHistoryItem.TransactionType.Transfer
            "0xa1903eab" -> TransactionHistoryItem.TransactionType.Submit
            "0x095ea7b3" -> TransactionHistoryItem.TransactionType.Approve
            "0x617ba037" -> TransactionHistoryItem.TransactionType.Supply
            "0x69328dec" -> TransactionHistoryItem.TransactionType.Withdraw
            "0xe8eda9df" -> TransactionHistoryItem.TransactionType.Deposit
            "0x12aa3caf" -> TransactionHistoryItem.TransactionType.Swap
            "0x0502b1c5", "0x2e95b6c8" -> TransactionHistoryItem.TransactionType.Unoswap
            else -> TransactionHistoryItem.TransactionType.Custom(id = methodId)
        }
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
                    .guard {
                        return null
                    }
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

    private fun extractTransactionDirection(
        isIncoming: Boolean,
        tx: GetAddressResponse.Transaction,
    ): TransactionHistoryItem.TransactionDirection {
        return if (isIncoming) {
            TransactionHistoryItem.TransactionDirection.Incoming(
                address = TransactionHistoryItem.Address.Single(tx.vin.first().addresses.first())
            )
        } else {
            TransactionHistoryItem.TransactionDirection.Outgoing(
                address = TransactionHistoryItem.Address.Single(tx.vout.first().addresses.first())
            )
        }
    }
}