package com.tangem.blockchain.transactionhistory.blockchains.tron

import com.tangem.Log
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.pagination.Page
import com.tangem.blockchain.common.pagination.PaginationWrapper
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.blockbook.network.BlockBookApi
import com.tangem.blockchain.network.blockbook.network.responses.GetAddressResponse
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryState
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem.TransactionType.TronStakingTransactionType
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryRequest
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
                    page = null,
                    pageSize = 1, // We don't need to know all transactions to define state
                    filterType = filterType,
                )
            }
            checkHistoryStatus(response, filterType)
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
                        page = request.pageToLoad,
                        pageSize = request.pageSize,
                        filterType = request.filterType,
                    )
                }
            val pageToLoad = request.pageToLoad?.toIntOrNull() ?: 0
            val page = response.page ?: 0

            // If response page is lower than request page, it means that we reached end. NowNodes works not
            // correctly.
            val isEndReached = page < pageToLoad
            if (isEndReached) {
                Result.Success(
                    PaginationWrapper(
                        nextPage = Page.LastPage,
                        items = emptyList(),
                    ),
                )
            } else {
                val txs = response.transactions
                    .orEmpty()
                    .mapNotNull { tx ->
                        tx.toTransactionHistoryItem(
                            walletAddress = request.address,
                            decimals = request.decimals,
                            filterType = request.filterType,
                        )
                    }
                val nextPage = if (response.page != null && request.page !is Page.LastPage) {
                    Page.Next(response.page.inc().toString())
                } else {
                    Page.LastPage
                }
                Result.Success(
                    PaginationWrapper(
                        nextPage = nextPage,
                        items = txs,
                    ),
                )
            }
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private fun checkHistoryStatus(
        response: GetAddressResponse,
        filterType: TransactionHistoryRequest.FilterType,
    ): TransactionHistoryState {
        return when (filterType) {
            TransactionHistoryRequest.FilterType.Coin -> {
                if (!response.transactions.isNullOrEmpty()) {
                    TransactionHistoryState.Success.HasTransactions(response.transactions.size)
                } else {
                    TransactionHistoryState.Success.Empty
                }
            }
            is TransactionHistoryRequest.FilterType.Contract -> {
                val token = response.trxTokens
                    ?.find { it.matching(filterType.tokenInfo.contractAddress) }
                    ?: return TransactionHistoryState.Success.Empty
                if (token.transfers != null && token.transfers > 0) {
                    TransactionHistoryState.Success.HasTransactions(token.transfers)
                } else {
                    TransactionHistoryState.Success.Empty
                }
            }
        }
    }

    private fun GetAddressResponse.TrxToken.matching(contractAddress: String): Boolean =
        listOf(this.id, this.name).any { it.equals(contractAddress, ignoreCase = true) }

    private fun GetAddressResponse.Transaction.toTransactionHistoryItem(
        walletAddress: String,
        decimals: Int,
        filterType: TransactionHistoryRequest.FilterType,
    ): TransactionHistoryItem? {
        val destinationType = extractDestinationType(
            tx = this,
            filterType = filterType,
            walletAddress = walletAddress,
        ).guard {
            Log.info { "Transaction $this doesn't contain a required value" }
            return null
        }
        val amount = extractAmount(
            tx = this,
            decimals = decimals,
            filterType = filterType,
            walletAddress = walletAddress,
        ).guard {
            Log.info { "Transaction $this doesn't contain a required value" }
            return null
        }
        if (shouldExcludeFromHistory(filterType, amount)) {
            Log.info { "Transaction with zero amount is excluded from history. $this" }
            return null
        }
        val sourceType = extractSourceType(
            tx = this,
            filterType = filterType,
            walletAddress = walletAddress,
        ).guard {
            Log.info { "Transaction $this doesn't contain a required value" }
            return null
        }
        val type = extractType(filterType = filterType, tx = this)
        return TransactionHistoryItem(
            txHash = txid.removePrefix(PREFIX),
            timestamp = TimeUnit.SECONDS.toMillis(blockTime.toLong()),
            isOutgoing = isOutgoing(type, walletAddress, sourceType),
            destinationType = destinationType,
            sourceType = sourceType,
            status = extractStatus(tx = this),
            type = type,
            amount = amount,
        )
    }

    private fun isOutgoing(
        type: TransactionHistoryItem.TransactionType,
        walletAddress: String,
        sourceType: TransactionHistoryItem.SourceType,
    ): Boolean {
        checkForStakingOutgoingTransaction(type)?.let { return it }

        return when (sourceType) {
            is TransactionHistoryItem.SourceType.Multiple -> {
                sourceType.addresses.any { it.equals(walletAddress, ignoreCase = true) }
            }
            is TransactionHistoryItem.SourceType.Single -> {
                sourceType.address.equals(walletAddress, ignoreCase = true)
            }
        }
    }

    private fun checkForStakingOutgoingTransaction(type: TransactionHistoryItem.TransactionType): Boolean? {
        return when (type) {
            is TronStakingTransactionType.VoteWitnessContract,
            TronStakingTransactionType.FreezeBalanceV2Contract,
            -> {
                true
            }
            TronStakingTransactionType.WithdrawBalanceContract,
            TronStakingTransactionType.WithdrawExpireUnfreezeContract,
            TronStakingTransactionType.UnfreezeBalanceV2Contract,
            -> {
                false
            }
            else -> {
                null
            }
        }
    }

    private fun extractDestinationType(
        tx: GetAddressResponse.Transaction,
        filterType: TransactionHistoryRequest.FilterType,
        walletAddress: String,
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
                val transfer = tx.getTokenTransfer(walletAddress, filterType.tokenInfo.contractAddress) ?: return null
                TransactionHistoryItem.DestinationType.Single(
                    addressType = TransactionHistoryItem.AddressType.User(transfer.to),
                )
            }
        }
    }

    private fun extractSourceType(
        tx: GetAddressResponse.Transaction,
        filterType: TransactionHistoryRequest.FilterType,
        walletAddress: String,
    ): TransactionHistoryItem.SourceType? {
        val address = when (filterType) {
            TransactionHistoryRequest.FilterType.Coin -> tx.fromAddress
            is TransactionHistoryRequest.FilterType.Contract -> {
                tx.getTokenTransfer(walletAddress, filterType.tokenInfo.contractAddress)?.from
            }
        }.guard { return null }

        return TransactionHistoryItem.SourceType.Single(address = address)
    }

    private fun extractType(
        filterType: TransactionHistoryRequest.FilterType,
        tx: GetAddressResponse.Transaction,
    ): TransactionHistoryItem.TransactionType {
        return when (filterType) {
            TransactionHistoryRequest.FilterType.Coin -> {
                when (tx.contractType) {
                    TRANSFER_CONTRACT_TYPE, TRANSFER_ASSET_CONTRACT_TYPE -> {
                        TransactionHistoryItem.TransactionType.Transfer
                    }
                    VOTE_WITNESS_CONTRACT_TYPE -> { // vote
                        TronStakingTransactionType.VoteWitnessContract(tx.voteList?.keys?.first().orEmpty())
                    }
                    WITHDRAW_BALANCE_CONTRACT_TYPE -> { // claim rewards
                        TronStakingTransactionType.WithdrawBalanceContract
                    }
                    FREEZE_BALANCE_V2_CONTRACT_TYPE -> { // freeze/stake
                        TronStakingTransactionType.FreezeBalanceV2Contract
                    }
                    UNFREEZE_BALANCE_V2_CONTRACT_TYPE -> { // unfreeze/unstake
                        TronStakingTransactionType.UnfreezeBalanceV2Contract
                    }
                    WITHDRAW_EXPIRE_UNFREEZE_CONTRACT_TYPE -> { // withdraw
                        TronStakingTransactionType.WithdrawExpireUnfreezeContract
                    }
                    else -> TransactionHistoryItem.TransactionType.ContractMethod(id = tx.contractName.orEmpty())
                }
            }
            is TransactionHistoryRequest.FilterType.Contract -> {
                // All TRC10 and TRC20 token transactions are considered simple & plain transfers
                TransactionHistoryItem.TransactionType.Transfer
            }
        }
    }

    private fun extractAmount(
        tx: GetAddressResponse.Transaction,
        decimals: Int,
        filterType: TransactionHistoryRequest.FilterType,
        walletAddress: String,
    ): Amount? {
        return when (filterType) {
            TransactionHistoryRequest.FilterType.Coin -> Amount(
                value = BigDecimal(tx.value).movePointLeft(blockchain.decimals()),
                blockchain = blockchain,
                type = AmountType.Coin,
            )

            is TransactionHistoryRequest.FilterType.Contract -> {
                val transfer = tx.getTokenTransfer(walletAddress, filterType.tokenInfo.contractAddress) ?: return null
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

    private fun extractStatus(tx: GetAddressResponse.Transaction): TransactionHistoryItem.TransactionStatus {
        val status = tx.tronTXReceipt?.status.guard {
            return if (tx.confirmations > 0) {
                TransactionHistoryItem.TransactionStatus.Confirmed
            } else {
                TransactionHistoryItem.TransactionStatus.Unconfirmed
            }
        }

        return when (status) {
            GetAddressResponse.Transaction.StatusType.PENDING -> TransactionHistoryItem.TransactionStatus.Unconfirmed
            GetAddressResponse.Transaction.StatusType.FAILURE -> TransactionHistoryItem.TransactionStatus.Failed
            GetAddressResponse.Transaction.StatusType.OK -> TransactionHistoryItem.TransactionStatus.Confirmed
        }
    }

    private fun GetAddressResponse.Transaction.getTokenTransfer(
        walletAddress: String,
        contractAddress: String,
    ): GetAddressResponse.Transaction.TokenTransfer? {
        return tokenTransfers.firstOrNull {
            contractAddress.equals(other = it.token, ignoreCase = true) &&
                walletAddress.equals(other = it.to, ignoreCase = true)
        }
    }

    private companion object {
        private const val TRANSFER_CONTRACT_TYPE = 1
        private const val TRANSFER_ASSET_CONTRACT_TYPE = 2
        private const val VOTE_WITNESS_CONTRACT_TYPE = 4 // vote
        private const val WITHDRAW_BALANCE_CONTRACT_TYPE = 13 // claim rewards
        private const val FREEZE_BALANCE_V2_CONTRACT_TYPE = 54 // freeze/stake
        private const val UNFREEZE_BALANCE_V2_CONTRACT_TYPE = 55 // unfreeze/unstake
        private const val WITHDRAW_EXPIRE_UNFREEZE_CONTRACT_TYPE = 56 // withdraw
    }
}