package com.tangem.blockchain.transactionhistory.blockchains.tron

import com.tangem.Log
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.pagination.Page
import com.tangem.blockchain.common.pagination.PaginationWrapper
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.blockbook.network.BlockBookApi
import com.tangem.blockchain.network.blockbook.network.responses.GetAddressResponse
import com.tangem.blockchain.network.blockbook.network.responses.feeAmount
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider.Companion.shouldExcludeFromHistory
import com.tangem.blockchain.transactionhistory.TransactionHistoryState
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem.TransactionType.TronStakingTransactionType
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryRequest
import com.tangem.blockchain.transactionhistory.utils.methodIdFromRawData
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.isZero
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

private const val PREFIX = "0x"

@Suppress("LargeClass")
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
        } catch (e: CancellationException) {
            throw e
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
                    .filterNot { tx -> shouldHideTechnicalTransaction(tx = tx, request = request) }
                    .mapNotNull { tx ->
                        tx.toTransactionHistoryItem(
                            walletAddress = request.address,
                            decimals = request.decimals,
                            filterType = request.filterType,
                        )
                    }
                    .filterNot { item -> shouldExcludeFromHistory(filterType = request.filterType, item = item) }
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    /**
     * Technical transactions that are not real coin movements and must not be shown in the coin history.
     *
     * The gasless flow rents energy for the wallet: the rental service delegates energy to the address right
     * before a gasless transfer and revokes it afterwards. None of that is initiated by the user, so it is
     * filtered out to keep the Tron history consistent with other networks.
     */
    private fun shouldHideTechnicalTransaction(
        tx: GetAddressResponse.Transaction,
        request: TransactionHistoryRequest,
    ): Boolean {
        if (request.filterType !is TransactionHistoryRequest.FilterType.Coin) return false

        return tx.isResourceRelated()
    }

    private fun GetAddressResponse.Transaction.isResourceRelated(): Boolean {
        contractType?.let { if (it in RESOURCE_CONTRACT_TYPES) return true }

        val payloadContractType = chainExtraData
            ?.takeIf { it.payloadType == TRON_PAYLOAD_TYPE }
            ?.payload
            ?.contractType
            ?: return false

        return payloadContractType in RESOURCE_PAYLOAD_TYPES
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
                    ?.find { it.contract.equals(filterType.tokenInfo.contractAddress, ignoreCase = true) }
                    ?: return TransactionHistoryState.Success.Empty
                if (token.transfers != null && token.transfers > 0) {
                    TransactionHistoryState.Success.HasTransactions(token.transfers)
                } else {
                    TransactionHistoryState.Success.Empty
                }
            }
        }
    }

    private fun GetAddressResponse.Transaction.toTransactionHistoryItem(
        walletAddress: String,
        decimals: Int,
        filterType: TransactionHistoryRequest.FilterType,
    ): TransactionHistoryItem? {
        val votes = extractVotes(tx = this)
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
            votes = votes,
        ).guard {
            Log.info { "Transaction $this doesn't contain a required value" }
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
        val type = if (contractType == null) {
            extractTypeV2(filterType = filterType, tx = this, votes = votes, walletAddress = walletAddress)
        } else {
            extractType(filterType = filterType, tx = this, votes = votes)
        }
        return TransactionHistoryItem(
            txHash = txid.removePrefix(PREFIX),
            timestamp = TimeUnit.SECONDS.toMillis(blockTime.toLong()),
            isOutgoing = isOutgoing(type, walletAddress, sourceType),
            destinationType = destinationType,
            sourceType = sourceType,
            status = extractStatus(tx = this),
            type = type,
            amount = amount,
            fee = extractFee(tx = this),
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

    /**
     * Withdraw and claim move TRX back to the account, but on-chain they are owner-initiated, so the generic
     * "source is the wallet" rule would wrongly call them outgoing. Every other staking operation is signed and
     * paid for by the owner, so that rule already answers correctly and decides on its own.
     */
    private fun checkForStakingOutgoingTransaction(type: TransactionHistoryItem.TransactionType): Boolean? {
        return when (type) {
            TronStakingTransactionType.WithdrawBalanceContract,
            TronStakingTransactionType.WithdrawExpireUnfreezeContract,
            -> false

            else -> null
        }
    }

    private fun extractDestinationType(
        tx: GetAddressResponse.Transaction,
        filterType: TransactionHistoryRequest.FilterType,
        walletAddress: String,
    ): TransactionHistoryItem.DestinationType? {
        return when (filterType) {
            TransactionHistoryRequest.FilterType.Coin -> {
                // Operations that send nothing anywhere - voting for a witness, claiming rewards - carry no
                // recipient in the Blockbook payload. They act on the account itself, so it stands in as the
                // destination; requiring one would drop those transactions from the history entirely.
                val toAddress = tx.toAddress
                    ?: tx.vout.firstOrNull()?.addresses?.firstOrNull()
                    ?: walletAddress
                TransactionHistoryItem.DestinationType.Single(
                    addressType = if (tx.tokenTransfers.isEmpty()) {
                        TransactionHistoryItem.AddressType.User(toAddress)
                    } else {
                        TransactionHistoryItem.AddressType.Contract(toAddress)
                    },
                )
            }

            is TransactionHistoryRequest.FilterType.Contract -> {
                val transfer = tx.getTokenTransfer(
                    walletAddress = walletAddress,
                    contractAddress = filterType.tokenInfo.contractAddress,
                ) ?: return null
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
            TransactionHistoryRequest.FilterType.Coin -> {
                tx.fromAddress ?: tx.vin.firstOrNull()?.addresses?.firstOrNull()
            }

            is TransactionHistoryRequest.FilterType.Contract -> {
                tx.getTokenTransfer(walletAddress, filterType.tokenInfo.contractAddress)?.from
            }
        }.guard { return null }

        return TransactionHistoryItem.SourceType.Single(address = address)
    }

    private fun extractType(
        filterType: TransactionHistoryRequest.FilterType,
        tx: GetAddressResponse.Transaction,
        votes: List<Vote>,
    ): TransactionHistoryItem.TransactionType {
        return when (filterType) {
            TransactionHistoryRequest.FilterType.Coin -> {
                when (tx.contractType) {
                    TRANSFER_CONTRACT_TYPE, TRANSFER_ASSET_CONTRACT_TYPE -> {
                        TransactionHistoryItem.TransactionType.Transfer
                    }

                    VOTE_WITNESS_CONTRACT_TYPE -> { // vote
                        TronStakingTransactionType.VoteWitnessContract(
                            validatorAddress = votes.firstOrNull()?.witness.orEmpty(),
                        )
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

    private fun extractTypeV2(
        filterType: TransactionHistoryRequest.FilterType,
        tx: GetAddressResponse.Transaction,
        votes: List<Vote>,
        walletAddress: String,
    ): TransactionHistoryItem.TransactionType {
        return when (filterType) {
            TransactionHistoryRequest.FilterType.Coin -> {
                val extraData =
                    tx.chainExtraData ?: return TransactionHistoryItem.TransactionType.Transfer

                if (extraData.payloadType != TRON_PAYLOAD_TYPE) {
                    return TransactionHistoryItem.TransactionType.Transfer
                }

                val contractType = extraData.payload?.contractType
                    ?: return TransactionHistoryItem.TransactionType.Transfer

                when (contractType) {
                    TronTxHistoryPayloadType.Transfer.type -> TransactionHistoryItem.TransactionType.Transfer

                    TronTxHistoryPayloadType.SmartContract.type -> {
                        extractContractCallType(tx = tx, walletAddress = walletAddress)
                    }

                    TronTxHistoryPayloadType.Vote.type -> { // vote
                        TronStakingTransactionType.VoteWitnessContract(
                            validatorAddress = votes.firstOrNull()?.witness.orEmpty(),
                        )
                    }

                    TronTxHistoryPayloadType.ClaimRewards.type -> { // claim rewards
                        TronStakingTransactionType.WithdrawBalanceContract
                    }

                    TronTxHistoryPayloadType.Freeze.type -> { // freeze/stake
                        TronStakingTransactionType.FreezeBalanceV2Contract
                    }

                    TronTxHistoryPayloadType.Unfreeze.type -> { // unfreeze/unstake
                        TronStakingTransactionType.UnfreezeBalanceV2Contract
                    }

                    TronTxHistoryPayloadType.Withdrawal.type -> { // withdraw
                        TronStakingTransactionType.WithdrawExpireUnfreezeContract
                    }

                    else -> TransactionHistoryItem.TransactionType.Transfer
                }
            }

            is TransactionHistoryRequest.FilterType.Contract -> {
                // All TRC10 and TRC20 token transactions are considered simple & plain transfers
                TransactionHistoryItem.TransactionType.Transfer
            }
        }
    }

    /**
     * A contract call moves no TRX, so in the coin history it survives only as a contract method — that is what
     * carries it past [shouldExcludeFromHistory], and the selector is what lets the app name the call and decode
     * the transfer the fee was paid for.
     *
     * Two calls stay a (zero) transfer and are dropped instead, because neither cost the wallet anything: one the
     * wallet did not sign — an incoming token transfer, whose fee belongs to its sender — and a gasless one, where
     * the rented energy covers the call and the payload reports no fee at all.
     */
    private fun extractContractCallType(
        tx: GetAddressResponse.Transaction,
        walletAddress: String,
    ): TransactionHistoryItem.TransactionType {
        if (!tx.isSentBy(walletAddress)) return TransactionHistoryItem.TransactionType.Transfer

        val paidFee = extractFee(tx).value ?: BigDecimal.ZERO
        if (paidFee.isZero()) return TransactionHistoryItem.TransactionType.Transfer

        val ethereumSpecific = tx.ethereumSpecific
        val methodId = ethereumSpecific?.parsedData?.methodId?.takeIf(String::isNotEmpty)
            ?: methodIdFromRawData(ethereumSpecific?.data)
            ?: return TransactionHistoryItem.TransactionType.Transfer

        if (methodId == HEX_PREFIX) return TransactionHistoryItem.TransactionType.Transfer

        return TransactionHistoryItem.TransactionType.ContractMethod(
            id = methodId,
            callData = ethereumSpecific?.data,
        )
    }

    private fun GetAddressResponse.Transaction.isSentBy(walletAddress: String): Boolean {
        val sender = fromAddress ?: vin.firstOrNull()?.addresses?.firstOrNull()

        return sender.equals(walletAddress, ignoreCase = true)
    }

    private fun extractAmount(
        tx: GetAddressResponse.Transaction,
        decimals: Int,
        filterType: TransactionHistoryRequest.FilterType,
        walletAddress: String,
        votes: List<Vote>,
    ): Amount? {
        return when (filterType) {
            TransactionHistoryRequest.FilterType.Coin -> Amount(
                value = extractCoinAmountValue(tx = tx, votes = votes),
                blockchain = blockchain,
                type = AmountType.Coin,
            )

            is TransactionHistoryRequest.FilterType.Contract -> {
                val transfer =
                    tx.getTokenTransfer(walletAddress, filterType.tokenInfo.contractAddress)
                        ?: return null
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

    /**
     * The transaction's own `value` stays zero for operations that move nothing between accounts, so the amount
     * they do carry has to be taken from the contract payload: a vote is backed by one frozen TRX, so the counts
     * arrive in whole TRX rather than in sun, and a claimed reward comes in its own field.
     */
    private fun extractCoinAmountValue(tx: GetAddressResponse.Transaction, votes: List<Vote>): BigDecimal {
        if (votes.isNotEmpty()) {
            return votes.sumOf { it.count?.toBigDecimalOrNull() ?: BigDecimal.ZERO }
        }

        val claimedReward = tx.chainExtraData?.payload?.claimedVoteReward?.toBigDecimalOrNull()
            ?: return BigDecimal(tx.value).movePointLeft(blockchain.decimals())

        return claimedReward.movePointLeft(blockchain.decimals())
    }

    /**
     * NowNodes reports the transaction's own `fees` as zero for every Tron transaction and delivers the charged
     * amount in the contract payload instead, so the payload wins whenever it carries one.
     */
    private fun extractFee(tx: GetAddressResponse.Transaction): Amount {
        val totalFee = tx.chainExtraData?.payload?.totalFee?.toBigDecimalOrNull()
            ?: return tx.feeAmount(blockchain)

        return Amount(value = totalFee.movePointLeft(blockchain.decimals()), blockchain = blockchain)
    }

    /** Votes come either in the `chainExtraData` payload or, for the legacy shape, in the `voteList` map. */
    private fun extractVotes(tx: GetAddressResponse.Transaction): List<Vote> {
        tx.chainExtraData?.payload?.votes?.let { payloadVotes ->
            return payloadVotes.map { Vote(witness = it.address, count = it.count) }
        }

        return tx.voteList.orEmpty()
            .toSortedMap()
            .map { (witness, count) -> Vote(witness = witness, count = count.toString()) }
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
        return tokenTransfers.firstOrNull { transfer ->
            contractAddress.equals(transfer.contract, ignoreCase = true) &&
                walletAddress.equals(transfer.to, ignoreCase = true) ||
                walletAddress.equals(transfer.from, ignoreCase = true)
        }
    }

    private data class Vote(val witness: String?, val count: String?)

    private companion object {
        private const val TRON_PAYLOAD_TYPE = "tron"

        private const val TRANSFER_CONTRACT_TYPE = 1
        private const val TRANSFER_ASSET_CONTRACT_TYPE = 2
        private const val VOTE_WITNESS_CONTRACT_TYPE = 4 // vote
        private const val ACCOUNT_CREATE_CONTRACT_TYPE = 9 // address activation
        private const val WITHDRAW_BALANCE_CONTRACT_TYPE = 13 // claim rewards
        private const val FREEZE_BALANCE_V2_CONTRACT_TYPE = 54 // freeze/stake
        private const val UNFREEZE_BALANCE_V2_CONTRACT_TYPE = 55 // unfreeze/unstake
        private const val WITHDRAW_EXPIRE_UNFREEZE_CONTRACT_TYPE = 56 // withdraw
        private const val DELEGATE_RESOURCE_CONTRACT_TYPE = 57 // energy/bandwidth delegation
        private const val UN_DELEGATE_RESOURCE_CONTRACT_TYPE = 58 // energy/bandwidth revocation

        /** Legacy `contract_type` values of transactions that are technical and never user-initiated. */
        private val RESOURCE_CONTRACT_TYPES = setOf(
            ACCOUNT_CREATE_CONTRACT_TYPE,
            DELEGATE_RESOURCE_CONTRACT_TYPE,
            UN_DELEGATE_RESOURCE_CONTRACT_TYPE,
        )

        /** Same as [RESOURCE_CONTRACT_TYPES], but for the `chainExtraData` payload shape. */
        private val RESOURCE_PAYLOAD_TYPES = setOf(
            TronTxHistoryPayloadType.AccountCreate.type,
            TronTxHistoryPayloadType.DelegateResource.type,
            TronTxHistoryPayloadType.UnDelegateResource.type,
        )
    }
}

enum class TronTxHistoryPayloadType(val type: String) {
    Transfer("TransferContract"),
    SmartContract("TriggerSmartContract"),

    // Staking related
    Unfreeze("UnfreezeBalanceV2Contract"),
    Vote("VoteWitnessContract"),
    Freeze("FreezeBalanceV2Contract"),
    Withdrawal("WithdrawExpireUnfreezeContract"),
    ClaimRewards("WithdrawBalanceContract"),

    // Technical, never initiated by the user. Produced by the gasless energy rental flow
    DelegateResource("DelegateResourceContract"),
    UnDelegateResource("UnDelegateResourceContract"),
    AccountCreate("AccountCreateContract"),
}