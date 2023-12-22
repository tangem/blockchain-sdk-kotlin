package com.tangem.blockchain.blockchains.ducatus

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.txhistory.TransactionHistoryProvider
import com.tangem.blockchain.extensions.Result
import java.math.BigDecimal

class DucatusWalletManager(
    wallet: Wallet,
    transactionBuilder: BitcoinTransactionBuilder,
    networkProvider: BitcoinNetworkProvider,
    transactionHistoryProvider: TransactionHistoryProvider,
) : BitcoinWalletManager(
    wallet = wallet,
    transactionHistoryProvider = transactionHistoryProvider,
    transactionBuilder = transactionBuilder,
    networkProvider = networkProvider,
),
    TransactionSender {

    override val minimalFeePerKb = 0.0001.toBigDecimal()
    override val minimalFee = 0.00001.toBigDecimal()

    override fun updateRecentTransactionsBasic(transactions: List<BasicTransactionData>) {
        if (transactions.isEmpty()) {
            wallet.recentTransactions.forEach { it.status = TransactionStatus.Confirmed }
        } else if (wallet.recentTransactions.find { it.status == TransactionStatus.Unconfirmed } == null) {
            wallet.addTransactionDummy()
        }
    }

    @Suppress("MagicNumber")
    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        val feeValue = BigDecimal.ONE.movePointLeft(blockchain.decimals())
        val sizeResult = transactionBuilder.getEstimateSize(
            TransactionData(amount, Fee.Common(Amount(amount, feeValue)), wallet.address, destination),
        )
        return when (sizeResult) {
            is Result.Failure -> sizeResult
            is Result.Success -> {
                val transactionSize = sizeResult.data.toBigDecimal()
                val minFee = BigDecimal.valueOf(0.00000089).multiply(transactionSize)
                val normalFee = BigDecimal.valueOf(0.00000144).multiply(transactionSize)
                val priorityFee = BigDecimal.valueOf(0.00000350).multiply(transactionSize)
                val fees = TransactionFee.Choosable(
                    minimum = Fee.Common(Amount(minFee, blockchain)),
                    normal = Fee.Common(Amount(normalFee, blockchain)),
                    priority = Fee.Common(Amount(priorityFee, blockchain)),
                )
                Result.Success(fees)
            }
        }
    }
}
