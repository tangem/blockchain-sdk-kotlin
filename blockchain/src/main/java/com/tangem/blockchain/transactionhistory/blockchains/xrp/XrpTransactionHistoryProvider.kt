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
                    .filterNot { item -> shouldExcludeFromHistory(filterType = request.filterType, item = item) }

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
        val amount = extractAmount(filterType) ?: return null
        val fee = feeInDrops?.movePointLeft(blockchain.decimals()) ?: return null

        return TransactionHistoryItem(
            txHash = hash,
            timestamp = date?.let { TimeUnit.SECONDS.toMillis(it + RIPPLE_EPOCH_OFFSET_SECONDS) } ?: 0L,
            isOutgoing = account == walletAddress,
            destinationType = TransactionHistoryItem.DestinationType.Single(
                addressType = TransactionHistoryItem.AddressType.User(extractDestination(walletAddress)),
            ),
            sourceType = TransactionHistoryItem.SourceType.Single(address = account),
            status = extractStatus(),
            type = extractType(),
            amount = amount,
            fee = Amount(blockchain = blockchain, value = fee),
        )
    }

    private fun XrpTransaction.extractAmount(filterType: TransactionHistoryRequest.FilterType): Amount? {
        return when (filterType) {
            TransactionHistoryRequest.FilterType.Coin -> {
                val drops = (amount as? XrpTransactionAmount.Drops)?.value ?: return null

                Amount(
                    value = drops.movePointLeft(blockchain.decimals()),
                    blockchain = blockchain,
                    type = AmountType.Coin,
                )
            }
            is TransactionHistoryRequest.FilterType.Contract -> {
                val token = filterType.tokenInfo
                val value = extractTokenAmount(token) ?: return null

                Amount(value = value, token = token)
            }
        }
    }

    /** Issued currency amounts are already denominated, so they need no scaling by the token decimals */
    private fun XrpTransaction.extractTokenAmount(token: Token): BigDecimal? {
        val issuedAmount = extractIssuedAmount() ?: return null
        val (currency, issuer) = parseAssetId(token.contractAddress) ?: return null

        return if (issuedAmount.currency == currency && issuedAmount.issuer == issuer) issuedAmount.value else null
    }

    private fun XrpTransaction.extractIssuedAmount(): XrpIssuedCurrencyAmount? {
        // `TrustSet` operations store the token data in `LimitAmount`
        return if (transactionType == TRUST_SET_TRANSACTION_TYPE) {
            limitAmount
        } else {
            (amount as? XrpTransactionAmount.IssuedCurrency)?.amount
        }
    }

    private fun parseAssetId(contractAddress: String): Pair<String, String>? {
        val parts = tokenAddressConverter.normalizeAddress(contractAddress)?.split(ASSET_ID_SEPARATOR)

        return if (parts?.size == ASSET_ID_PARTS_COUNT) parts[0] to parts[1] else null
    }

    private fun XrpTransaction.extractDestination(walletAddress: String): String {
        destination?.let { return it }

        if (transactionType == TRUST_SET_TRANSACTION_TYPE) {
            limitAmount?.issuer?.let { return it }
        }

        return walletAddress
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

    private companion object {

        // We don't need to know all transactions to define state
        const val STATE_CHECK_LIMIT = 1

        const val PAYMENT_TRANSACTION_TYPE = "Payment"
        const val TRUST_SET_TRANSACTION_TYPE = "TrustSet"
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