package com.tangem.blockchain.transactionhistory.blockchains.koinos

import com.tangem.blockchain.blockchains.koinos.models.KoinosTransactionEntry
import com.tangem.blockchain.blockchains.koinos.network.KoinosNetworkService
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.pagination.Page
import com.tangem.blockchain.common.pagination.PaginationWrapper
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryState
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryRequest
import java.math.BigInteger

@Deprecated(
    """
    Does not work properly due to the lack of a timestamp in the transaction history record.
    Transacrtion history api response: 
    com.tandem.blockchain.blockchains.koinos.network.to.KoinosMethod.GetAccountHistory.Response
    Maybe that will change someday.
 """,
    level = DeprecationLevel.ERROR,
)
internal class KoinosTransactionHistoryProvider(
    private val networkService: KoinosNetworkService,
) : TransactionHistoryProvider {

    override suspend fun getTransactionHistoryState(
        address: String,
        filterType: TransactionHistoryRequest.FilterType,
    ): TransactionHistoryState {
        val nonceResult = networkService.getCurrentNonce(address)
            .successOr { return TransactionHistoryState.Failed.FetchError(exception = it.error) }

        val nonce = nonceResult.nonce

        // mb change txCount type to BigInteger or Long?
        return if (nonce > BigInteger.ZERO) {
            TransactionHistoryState.Success.HasTransactions(txCount = nonce.toInt())
        } else {
            TransactionHistoryState.Success.Empty
        }
    }

    override suspend fun getTransactionsHistory(
        request: TransactionHistoryRequest,
    ): Result<PaginationWrapper<TransactionHistoryItem>> {
        if (request.filterType !is TransactionHistoryRequest.FilterType.Coin) {
            return Result.Failure(IllegalStateException("No pages to load").toBlockchainSdkError())
        }

        val sequenceNum = if (request.lastSequenceNumber() == 0L) {
            0
        } else {
            request.lastSequenceNumber() + request.pageSize
        }

        val transactions = networkService.getTransactionHistory(
            address = request.address,
            pageSize = request.pageSize,
            sequenceNum = sequenceNum,
        ).successOr { return it }

        val nextPage = if (transactions.size == request.pageSize) {
            Page.Next(transactions[0].sequenceNum.toString())
        } else {
            Page.LastPage
        }

        val transactionHistoryItems = transactions.mapNotNull {
            val event = when (it.event) {
                is KoinosTransactionEntry.Event.KoinTransferEvent -> {
                    val destination = it.event.fromAddress.getDestinationAddress()
                    val source = it.event.toAddress.getSourceAddress()

                    source to destination
                }
                KoinosTransactionEntry.Event.Unsupported -> {
                    TransactionHistoryItem.SourceType.Single(
                        it.payerAddress,
                    ) to TransactionHistoryItem.DestinationType.Single(
                        addressType = TransactionHistoryItem.AddressType.User(""),
                    )
                }
            }

            val amount = when (it.event) {
                is KoinosTransactionEntry.Event.KoinTransferEvent -> {
                    Amount(it.event.value.toBigDecimal().movePointLeft(request.decimals), Blockchain.Koinos)
                }
                KoinosTransactionEntry.Event.Unsupported -> Amount(Blockchain.Koinos)
            }

            // TODO maybe add mana field as additional info | it.rcUsed
            TransactionHistoryItem(
                txHash = it.id,
                timestamp = 0L,
                isOutgoing = false,
                sourceType = event.first,
                destinationType = event.second,
                amount = amount,
                status = TransactionHistoryItem.TransactionStatus.Confirmed,
                type = TransactionHistoryItem.TransactionType.Transfer,
            )
        }

        return Result.Success(PaginationWrapper(nextPage = nextPage, items = transactionHistoryItems))
    }

    private fun KoinosTransactionEntry.Address.getSourceAddress(): TransactionHistoryItem.SourceType {
        return when (this) {
            is KoinosTransactionEntry.Address.Multiple -> {
                TransactionHistoryItem.SourceType.Multiple(addresses)
            }
            is KoinosTransactionEntry.Address.Single -> {
                TransactionHistoryItem.SourceType.Single(address)
            }
        }
    }

    private fun KoinosTransactionEntry.Address.getDestinationAddress(): TransactionHistoryItem.DestinationType {
        return when (this) {
            is KoinosTransactionEntry.Address.Multiple -> {
                TransactionHistoryItem.DestinationType.Multiple(
                    addresses.map { TransactionHistoryItem.AddressType.User(it) },
                )
            }
            is KoinosTransactionEntry.Address.Single -> {
                TransactionHistoryItem.DestinationType.Single(
                    TransactionHistoryItem.AddressType.User(address),
                )
            }
        }
    }

    private fun TransactionHistoryRequest.lastSequenceNumber(): Long = when (page) {
        Page.Initial -> 0
        is Page.Next -> page.value.toLong()
        Page.LastPage -> error("Not supposed to happen")
    }
}