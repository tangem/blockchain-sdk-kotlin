package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.blockchains.ethereum.network.EthereumInfoResponse
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
import java.util.Calendar

/**
 * The network provider of an EVM address the wallet does not own — a contract account such as a Safe.
 *
 * Every [EthereumNetworkProvider] and [TransactionHistoryProvider] call goes to the chain's own services; on top of
 * that the provider keeps the address's state the way a wallet manager keeps its wallet: [update] refreshes the coin
 * and token [amounts] and reconciles [recentTransactions] with what the network reports, [addTransactions] lets the
 * host enrich that list from its own sources. There is no key, no signing and no sending — the address is given,
 * never derived.
 *
[REDACTED_AUTHOR]
 *
 * @property blockchain the chain the address is observed on
 * @property address    the observed address
 */
class ExternalAddressBalanceProvider internal constructor(
    val blockchain: Blockchain,
    val address: String,
    private val networkProvider: EthereumNetworkProvider,
    transactionHistoryProvider: TransactionHistoryProvider,
) : EthereumNetworkProvider by networkProvider,
    TransactionHistoryProvider by transactionHistoryProvider {

    private val mutableTokens: MutableSet<Token> = mutableSetOf()
    private val mutableAmounts: MutableMap<AmountType, Amount> = mutableMapOf()
    private val mutableRecentTransactions: MutableList<TransactionData.Uncompiled> = mutableListOf()

    private var txCount = -1L
    private var pendingTxCount = -1L

    /** Tokens whose balances [update] reads together with the coin; changed only through [addTokens] and [removeToken] */
    val tokens: Set<Token>
        get() = mutableTokens.toSet()

    /** Coin and token balances as of the last successful [update]; empty until then */
    val amounts: Map<AmountType, Amount>
        get() = mutableAmounts

    /**
     * Transactions of the address, unconfirmed ones included: what the network reported on [update] plus what the host
     * added through [addTransactions]. Entries are never dropped, only marked [TransactionStatus.Confirmed] — the same
     * contract as `Wallet.recentTransactions`.
     */
    val recentTransactions: List<TransactionData.Uncompiled>
        get() = mutableRecentTransactions

    fun addTokens(tokens: Collection<Token>) {
        mutableTokens.addAll(tokens)
    }

    fun removeToken(token: Token) {
        mutableTokens.remove(token)
        mutableAmounts.keys.removeAll { type -> type is AmountType.Token && type.token == token }
    }

    /**
     * Enriches [recentTransactions] from a source of the host's own — for instance the operations of a Safe known to
     * the backend. A transaction with a hash already present replaces that entry (so the host can update a status),
     * the rest are appended.
     */
    fun addTransactions(transactions: List<TransactionData.Uncompiled>) {
        transactions.forEach { transaction ->
            val index = if (transaction.hash == null) {
                -1
            } else {
                mutableRecentTransactions.indexOfFirst { it.hash.equals(transaction.hash, ignoreCase = true) }
            }

            if (index >= 0) {
                mutableRecentTransactions[index] = transaction
            } else {
                mutableRecentTransactions.add(transaction)
            }
        }
    }

    /**
     * Reads the address through [getInfo] and stores the answer: coin and token balances in [amounts], transaction
     * counts and recent transactions reconciled into [recentTransactions] exactly as an EVM wallet manager does.
     *
     * The network service belongs to this provider and is built without the wallet-scoped yield-supply module, so
     * [getInfo] answers for the address itself.
     */
    suspend fun update(): Result<ExternalAddressInfo> {
        return when (val result = getInfo(address = address, tokens = mutableTokens.toSet())) {
            is Result.Success -> {
                updateAmounts(result.data)
                updateTransactions(result.data)

                Result.Success(info())
            }
            is Result.Failure -> Result.Failure(result.error)
        }
    }

    private fun updateAmounts(data: EthereumInfoResponse) {
        mutableAmounts.clear()
        mutableAmounts[AmountType.Coin] = Amount(data.coinBalance, blockchain, AmountType.Coin)
        data.tokenBalances.forEach { amount -> mutableAmounts[amount.type] = amount }
    }

    private fun updateTransactions(data: EthereumInfoResponse) {
        txCount = data.txCount
        pendingTxCount = data.pendingTxCount

        if (txCount == pendingTxCount) {
            mutableRecentTransactions.forEach { it.status = TransactionStatus.Confirmed }
        } else if (!data.recentTransactions.isNullOrEmpty()) {
            updateRecentTransactions(data.recentTransactions)
        } else {
            addTransactionDummy()
        }
    }

    private fun updateRecentTransactions(transactions: List<TransactionData.Uncompiled>) {
        val (confirmed, unconfirmed) = transactions.partition { it.status == TransactionStatus.Confirmed }

        mutableRecentTransactions.forEach { recent ->
            if (confirmed.any { it.hash.equals(recent.hash, true) }) recent.status = TransactionStatus.Confirmed
        }
        unconfirmed.forEach { transaction ->
            if (mutableRecentTransactions.none { it.hash.equals(transaction.hash, true) }) {
                mutableRecentTransactions.add(transaction)
            }
        }
    }

    private fun addTransactionDummy() {
        mutableRecentTransactions.add(
            TransactionData.Uncompiled(
                amount = Amount(null, blockchain),
                fee = null,
                sourceAddress = UNKNOWN_ADDRESS,
                destinationAddress = UNKNOWN_ADDRESS,
                date = Calendar.getInstance(),
            ),
        )
    }

    private fun info(): ExternalAddressInfo {
        return ExternalAddressInfo(
            address = address,
            amounts = mutableAmounts.toMap(),
            recentTransactions = mutableRecentTransactions.toList(),
        )
    }

    private companion object {
        const val UNKNOWN_ADDRESS = "unknown"
    }
}