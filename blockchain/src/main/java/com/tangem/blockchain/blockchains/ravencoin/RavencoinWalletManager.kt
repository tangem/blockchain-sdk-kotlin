package com.tangem.blockchain.blockchains.ravencoin

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.txhistory.TransactionHistoryProvider
import java.math.BigDecimal

class RavencoinWalletManager(
    wallet: Wallet,
    transactionBuilder: BitcoinTransactionBuilder,
    networkProvider: BitcoinNetworkProvider,
    transactionHistoryProvider: TransactionHistoryProvider,
) : BitcoinWalletManager(wallet, transactionHistoryProvider, transactionBuilder, networkProvider) {

    // https://github.com/raven-community/ravencore-lib/blob/master/docs/transaction.md
    override val minimalFeePerKb: BigDecimal = BigDecimal(10_000).movePointLeft(wallet.blockchain.decimals())
    override val dustValue: BigDecimal = BigDecimal(642).movePointLeft(wallet.blockchain.decimals())

    override fun updateRecentTransactionsBasic(transactions: List<BasicTransactionData>) {
        val (confirmedTransactions, unconfirmedTransactions) =
            transactions.partition { it.isConfirmed }

        wallet.recentTransactions.forEach { recent ->
            if (confirmedTransactions.containsRecent(recent)) {
                recent.status = TransactionStatus.Confirmed
            }
        }
        unconfirmedTransactions.forEach { unconfirmed ->
            val recentTx = wallet.recentTransactions.find { recent -> recent.hash.equals(unconfirmed.hash, true) }
            if (recentTx == null) {
                wallet.recentTransactions.add(unconfirmed.toTransactionData())
            }
        }
    }

    private fun List<BasicTransactionData>.containsRecent(recent: TransactionData): Boolean =
        this.find { confirmed -> confirmed.hash.equals(recent.hash, true) } != null

    private fun BasicTransactionData.toTransactionData(): TransactionData {
        val coinAmount = requireNotNull(wallet.amounts[AmountType.Coin]) { "Coin amount must not be null" }
        return TransactionData(
            amount = Amount(coinAmount, this.balanceDif.abs()),
            fee = null,
            sourceAddress = source,
            destinationAddress = destination,
            hash = this.hash,
            date = this.date,
            status = if (this.isConfirmed) TransactionStatus.Confirmed else TransactionStatus.Unconfirmed,
        )
    }
}
