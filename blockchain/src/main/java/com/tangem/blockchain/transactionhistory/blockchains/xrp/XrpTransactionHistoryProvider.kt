package com.tangem.blockchain.transactionhistory.blockchains.xrp

import com.tangem.blockchain.blockchains.xrp.XrpTokenAddressConverter
import com.tangem.blockchain.blockchains.xrp.network.XrpAccountTxRequest
import com.tangem.blockchain.blockchains.xrp.network.XrpIssuedCurrencyAmount
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkProvider
import com.tangem.blockchain.blockchains.xrp.network.XrpTransaction
import com.tangem.blockchain.blockchains.xrp.network.XrpTransactionAmount
import com.tangem.blockchain.blockchains.xrp.network.XrpTransactionMarker
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.pagination.Page
import com.tangem.blockchain.common.pagination.PaginationWrapper
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider.Companion.shouldExcludeFromHistory
import com.tangem.blockchain.transactionhistory.TransactionHistoryState
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryRequest
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

/**
 * Transaction history provider of the XRP Ledger. Backed by the `account_tx` rippled method.
 *
 * @property blockchain      XRP blockchain
 * @property networkProvider provider of the `account_tx` method
 */
internal class XrpTransactionHistoryProvider(
    private val blockchain: Blockchain,
    private val networkProvider: XrpNetworkProvider,
) : TransactionHistoryProvider {

    private val tokenAddressConverter = XrpTokenAddressConverter()

    override suspend fun getTransactionHistoryState(
        address: String,
        filterType: TransactionHistoryRequest.FilterType,
    ): TransactionHistoryState {
        val request = XrpAccountTxRequest(address = address, limit = STATE_CHECK_LIMIT)

        return when (val result = networkProvider.getAccountTransactions(request)) {
            is Result.Success -> {
                val transactions = result.data.transactions

                if (transactions.isEmpty()) {
                    TransactionHistoryState.Success.Empty
                } else {
                    TransactionHistoryState.Success.HasTransactions(transactions.size)
                }
            }
            is Result.Failure -> TransactionHistoryState.Failed.FetchError(
                exception = result.error as? Exception ?: Exception(result.error.customMessage),
            )
        }
    }

    override suspend fun getTransactionsHistory(
        request: TransactionHistoryRequest,
    ): Result<PaginationWrapper<TransactionHistoryItem>> {
        val accountTxRequest = XrpAccountTxRequest(
            address = request.address,
            limit = request.pageSize,
            marker = request.pageToLoad?.let(::decodeMarker),
        )

        return when (val result = networkProvider.getAccountTransactions(accountTxRequest)) {
            is Result.Success -> {
                val items = result.data.transactions
                    .mapNotNull { it.toTransactionHistoryItem(request.address, request.filterType) }
                    .filterNot { item ->
                        // Only plain transfers are subject to the shared zero-amount filter. The other supported
                        // types report a zero amount by design, yet they belong in the history of their currency
                        item.type == TransactionHistoryItem.TransactionType.Transfer &&
                            shouldExcludeFromHistory(filterType = request.filterType, item = item)
                    }

                Result.Success(
                    PaginationWrapper(
                        nextPage = result.data.marker?.let { Page.Next(encodeMarker(it)) } ?: Page.LastPage,
                        items = items,
                    ),
                )
            }
            is Result.Failure -> result
        }
    }

    private fun XrpTransaction.toTransactionHistoryItem(
        walletAddress: String,
        filterType: TransactionHistoryRequest.FilterType,
    ): TransactionHistoryItem? {
        if (transactionType !in SUPPORTED_TRANSACTION_TYPES) return null

        val historyAmount = extractHistoryAmount(walletAddress, filterType) ?: return null
        val fee = feeInDrops?.movePointLeft(blockchain.decimals()) ?: return null

        return TransactionHistoryItem(
            txHash = hash,
            timestamp = date?.let { TimeUnit.SECONDS.toMillis(it + RIPPLE_EPOCH_OFFSET_SECONDS) } ?: 0L,
            isOutgoing = historyAmount.isOutgoing,
            destinationType = TransactionHistoryItem.DestinationType.Single(
                addressType = TransactionHistoryItem.AddressType.User(extractDestination(walletAddress)),
            ),
            sourceType = TransactionHistoryItem.SourceType.Single(address = account),
            status = extractStatus(),
            type = extractType(),
            amount = historyAmount.toAmount(filterType),
            fee = Amount(blockchain = blockchain, value = fee),
        )
    }

    private fun XrpHistoryAmount.toAmount(filterType: TransactionHistoryRequest.FilterType): Amount {
        return when (filterType) {
            TransactionHistoryRequest.FilterType.Coin -> Amount(
                value = value,
                blockchain = blockchain,
                type = AmountType.Coin,
            )
            is TransactionHistoryRequest.FilterType.Contract -> Amount(value = value, token = filterType.tokenInfo)
        }
    }

    private fun XrpTransaction.extractHistoryAmount(
        walletAddress: String,
        filterType: TransactionHistoryRequest.FilterType,
    ): XrpHistoryAmount? {
        return when (filterType) {
            TransactionHistoryRequest.FilterType.Coin -> extractCoinAmount(walletAddress)
            is TransactionHistoryRequest.FilterType.Contract -> extractTokenAmount(walletAddress, filterType.tokenInfo)
        }
    }

    private fun XrpTransaction.extractCoinAmount(walletAddress: String): XrpHistoryAmount? {
        val isOutgoing = account == walletAddress

        return when (transactionType) {
            // Account creation is a `Payment` too, so it needs no branch of its own
            PAYMENT_TRANSACTION_TYPE, ESCROW_CREATE_TRANSACTION_TYPE -> {
                val drops = (amount as? XrpTransactionAmount.Drops)?.value ?: return null

                XrpHistoryAmount(value = drops.movePointLeft(blockchain.decimals()), isOutgoing = isOutgoing)
            }
            OFFER_CREATE_TRANSACTION_TYPE -> {
                val side = matchOfferSide { it is XrpTransactionAmount.Drops } ?: return null

                XrpHistoryAmount(value = BigDecimal.ZERO, isOutgoing = side.isOutgoing)
            }
            // These types move funds through the metadata only, so the moved value stays unknown here
            OFFER_CANCEL_TRANSACTION_TYPE,
            ESCROW_FINISH_TRANSACTION_TYPE,
            ESCROW_CANCEL_TRANSACTION_TYPE,
            ACCOUNT_DELETE_TRANSACTION_TYPE,
            -> XrpHistoryAmount(value = BigDecimal.ZERO, isOutgoing = isOutgoing)
            // `TrustSet` and `Clawback` always belong to an issued currency, never to XRP
            else -> null
        }
    }

    /** Issued currency amounts are already denominated, so they need no scaling by the token decimals */
    private fun XrpTransaction.extractTokenAmount(walletAddress: String, token: Token): XrpHistoryAmount? {
        val (currency, issuer) = parseAssetId(token.contractAddress) ?: return null
        val isOutgoing = account == walletAddress

        return when (transactionType) {
            PAYMENT_TRANSACTION_TYPE, ESCROW_CREATE_TRANSACTION_TYPE -> {
                val issued = (amount as? XrpTransactionAmount.IssuedCurrency)?.amount ?: return null
                if (!issued.matches(currency, issuer)) return null

                XrpHistoryAmount(value = issued.value, isOutgoing = isOutgoing)
            }
            TRUST_SET_TRANSACTION_TYPE -> {
                if (limitAmount?.matches(currency, issuer) != true) return null

                // `LimitAmount` is the trust line cap, not a transferred amount, so nothing is moved
                XrpHistoryAmount(value = BigDecimal.ZERO, isOutgoing = isOutgoing)
            }
            OFFER_CREATE_TRANSACTION_TYPE -> {
                val side = matchOfferSide {
                    (it as? XrpTransactionAmount.IssuedCurrency)?.amount?.matches(currency, issuer) == true
                } ?: return null

                XrpHistoryAmount(value = BigDecimal.ZERO, isOutgoing = side.isOutgoing)
            }
            CLAWBACK_TRANSACTION_TYPE -> {
                // The issuer claws the funds back, so it is `Account`, while `Amount.issuer` holds the account
                // the funds are taken from
                val issued = (amount as? XrpTransactionAmount.IssuedCurrency)?.amount ?: return null
                if (issued.currency != currency || account != issuer) return null

                XrpHistoryAmount(value = issued.value, isOutgoing = issued.issuer == walletAddress)
            }
            // The remaining types carry no currency data, so they cannot be attributed to a token
            else -> null
        }
    }

    /**
     * Side of an `OfferCreate` the [predicate] matches, or `null` when the offer doesn't involve the requested
     * currency at all. `TakerGets` is sold by the offer owner, `TakerPays` is bought by them.
     */
    private fun XrpTransaction.matchOfferSide(predicate: (XrpTransactionAmount) -> Boolean): XrpOfferSide? = when {
        takerGets?.let(predicate) == true -> XrpOfferSide.SELL
        takerPays?.let(predicate) == true -> XrpOfferSide.BUY
        else -> null
    }

    private fun XrpIssuedCurrencyAmount.matches(currency: String, issuer: String): Boolean {
        return this.currency == currency && this.issuer == issuer
    }

    private fun parseAssetId(contractAddress: String): Pair<String, String>? {
        val parts = tokenAddressConverter.normalizeAddress(contractAddress)?.split(ASSET_ID_SEPARATOR)

        return if (parts?.size == ASSET_ID_PARTS_COUNT) parts[0] to parts[1] else null
    }

    private fun XrpTransaction.extractDestination(walletAddress: String): String {
        destination?.let { return it }

        val counterparty = when (transactionType) {
            TRUST_SET_TRANSACTION_TYPE -> limitAmount?.issuer
            // The clawback is performed by the issuer of the token
            CLAWBACK_TRANSACTION_TYPE -> account
            else -> null
        }

        return counterparty ?: walletAddress
    }

    private fun XrpTransaction.extractStatus(): TransactionHistoryItem.TransactionStatus = when {
        transactionResult != null && transactionResult != SUCCESS_RESULT ->
            TransactionHistoryItem.TransactionStatus.Failed
        isValidated -> TransactionHistoryItem.TransactionStatus.Confirmed
        else -> TransactionHistoryItem.TransactionStatus.Unconfirmed
    }

    private fun XrpTransaction.extractType(): TransactionHistoryItem.TransactionType {
        return if (transactionType == PAYMENT_TRANSACTION_TYPE) {
            TransactionHistoryItem.TransactionType.Transfer
        } else {
            TransactionHistoryItem.TransactionType.ContractMethodName(name = transactionType)
        }
    }

    private fun encodeMarker(marker: XrpTransactionMarker): String = "${marker.ledger}$MARKER_SEPARATOR${marker.seq}"

    private fun decodeMarker(value: String): XrpTransactionMarker? {
        val parts = value.split(MARKER_SEPARATOR)
        if (parts.size != MARKER_PARTS_COUNT) return null

        val ledger = parts[0].toLongOrNull() ?: return null
        val seq = parts[1].toLongOrNull() ?: return null

        return XrpTransactionMarker(ledger = ledger, seq = seq)
    }

    /**
     * Amount of a history item, already attributed to the requested currency.
     *
     * @property value      value moved by the transaction. `ZERO` for the types that move no funds and for those
     *                      whose moved value is declared by the metadata rather than by the transaction itself
     * @property isOutgoing whether the funds leave the wallet
     */
    private data class XrpHistoryAmount(val value: BigDecimal, val isOutgoing: Boolean)

    private enum class XrpOfferSide(val isOutgoing: Boolean) {
        SELL(isOutgoing = true),
        BUY(isOutgoing = false),
    }

    private companion object {

        // We don't need to know all transactions to define state
        const val STATE_CHECK_LIMIT = 1

        const val PAYMENT_TRANSACTION_TYPE = "Payment"
        const val TRUST_SET_TRANSACTION_TYPE = "TrustSet"
        const val OFFER_CREATE_TRANSACTION_TYPE = "OfferCreate"
        const val OFFER_CANCEL_TRANSACTION_TYPE = "OfferCancel"
        const val ACCOUNT_DELETE_TRANSACTION_TYPE = "AccountDelete"
        const val ESCROW_CREATE_TRANSACTION_TYPE = "EscrowCreate"
        const val ESCROW_FINISH_TRANSACTION_TYPE = "EscrowFinish"
        const val ESCROW_CANCEL_TRANSACTION_TYPE = "EscrowCancel"
        const val CLAWBACK_TRANSACTION_TYPE = "Clawback"

        /**
         * Types the history renders. Everything else — NFTs, checks, multisig, tickets and the other service
         * operations — is left out on purpose.
         */
        val SUPPORTED_TRANSACTION_TYPES = setOf(
            PAYMENT_TRANSACTION_TYPE,
            TRUST_SET_TRANSACTION_TYPE,
            OFFER_CREATE_TRANSACTION_TYPE,
            OFFER_CANCEL_TRANSACTION_TYPE,
            ACCOUNT_DELETE_TRANSACTION_TYPE,
            ESCROW_CREATE_TRANSACTION_TYPE,
            ESCROW_FINISH_TRANSACTION_TYPE,
            ESCROW_CANCEL_TRANSACTION_TYPE,
            CLAWBACK_TRANSACTION_TYPE,
        )

        const val SUCCESS_RESULT = "tesSUCCESS"

        /**
         * Offset between the Ripple Epoch and the Unix Epoch.
         * https://xrpl.org/docs/references/protocol/data-types/basic-data-types#specifying-time
         */
        const val RIPPLE_EPOCH_OFFSET_SECONDS = 946_684_800L

        const val ASSET_ID_SEPARATOR = "."
        const val ASSET_ID_PARTS_COUNT = 2

        const val MARKER_SEPARATOR = ":"
        const val MARKER_PARTS_COUNT = 2
    }
}