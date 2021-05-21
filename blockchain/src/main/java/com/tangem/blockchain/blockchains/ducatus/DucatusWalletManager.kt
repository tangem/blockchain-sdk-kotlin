package com.tangem.blockchain.blockchains.ducatus

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import org.bitcoinj.core.TransactionInput
import java.math.BigDecimal

class DucatusWalletManager(
        wallet: Wallet,
        transactionBuilder: BitcoinTransactionBuilder,
        networkProvider: BitcoinNetworkProvider
) : BitcoinWalletManager(wallet, transactionBuilder, networkProvider), TransactionSender {

    override fun updateRecentTransactionsBasic(transactions: List<BasicTransactionData>) {
        if (transactions.isEmpty()) {
            wallet.recentTransactions.forEach { it.status = TransactionStatus.Confirmed }
        } else if (wallet.recentTransactions.find { it.status == TransactionStatus.Unconfirmed } == null) {
            wallet.addTransactionDummy()
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        val feeValue = BigDecimal.ONE.movePointLeft(blockchain.decimals())
        val sizeResult = transactionBuilder.getEstimateSize(
                TransactionData(amount, Amount(amount, feeValue), wallet.address, destination)
        )
        return when (sizeResult) {
            is Result.Failure -> sizeResult
            is Result.Success -> {
                val transactionSize = sizeResult.data.toBigDecimal()
                val minFee = BigDecimal.valueOf(0.00000089).multiply(transactionSize)
                val normalFee = BigDecimal.valueOf(0.00000144).multiply(transactionSize)
                val priorityFee = BigDecimal.valueOf(0.00000350).multiply(transactionSize)
                val fees = listOf(
                        Amount(minFee, blockchain),
                        Amount(normalFee, blockchain),
                        Amount(priorityFee, blockchain)
                )
                Result.Success(fees)
            }
        }
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner) =
            send(transactionData, signer, TransactionInput.NO_SEQUENCE)

    override suspend fun isPushAvailable(transactionHash: String) = Result.Success(false)
}