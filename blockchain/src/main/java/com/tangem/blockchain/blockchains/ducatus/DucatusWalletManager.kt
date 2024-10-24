package com.tangem.blockchain.blockchains.ducatus

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
import com.tangem.blockchain.extensions.Result
import java.math.BigDecimal

internal class DucatusWalletManager(
    wallet: Wallet,
    transactionBuilder: BitcoinTransactionBuilder,
    networkProvider: BitcoinNetworkProvider,
    transactionHistoryProvider: TransactionHistoryProvider,
    private val feesCalculator: DucatusFeesCalculator,
) : BitcoinWalletManager(
    wallet = wallet,
    transactionHistoryProvider = transactionHistoryProvider,
    transactionBuilder = transactionBuilder,
    networkProvider = networkProvider,
    feesCalculator = feesCalculator,
),
    TransactionSender {

    override fun updateRecentTransactionsBasic(transactions: List<BasicTransactionData>) {
        if (transactions.isEmpty()) {
            wallet.recentTransactions.forEach { it.status = TransactionStatus.Confirmed }
        } else if (wallet.recentTransactions.find { it.status == TransactionStatus.Unconfirmed } == null) {
            wallet.addTransactionDummy()
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        val feeValue = BigDecimal.ONE.movePointLeft(blockchain.decimals())
        val sizeResult = transactionBuilder.getEstimateSize(
            TransactionData.Uncompiled(amount, Fee.Common(Amount(amount, feeValue)), wallet.address, destination),
        )
        return when (sizeResult) {
            is Result.Failure -> sizeResult
            is Result.Success -> {
                val transactionSize = sizeResult.data.toBigDecimal()
                val fees = feesCalculator.calculateFees(transactionSize)
                Result.Success(fees)
            }
        }
    }
}
