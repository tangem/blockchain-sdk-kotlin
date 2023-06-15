package com.tangem.blockchain.blockchains.cardano

import android.util.Log
import com.tangem.blockchain.blockchains.cardano.network.CardanoAddressResponse
import com.tangem.blockchain.blockchains.cardano.network.CardanoNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import java.math.BigDecimal
import java.math.RoundingMode

class CardanoWalletManager(
    wallet: Wallet,
    private val transactionBuilder: CardanoTransactionBuilder,
    private val networkProvider: CardanoNetworkProvider,
) : WalletManager(wallet), TransactionSender {

    override val dustValue: BigDecimal = BigDecimal.ONE
    private val blockchain = wallet.blockchain

    override val currentHost: String
        get() = networkProvider.host

    override suspend fun update() {
        when (val response = networkProvider.getInfo(wallet.addresses.map { it.value }.toSet())) {
            is Result.Success -> updateWallet(response.data)
            is Result.Failure -> updateError(response.error)
        }
    }

    private fun updateWallet(response: CardanoAddressResponse) {
        Log.d(this::class.java.simpleName, "Balance is ${response.balance}")

        wallet.changeAmountValue(
            amountType = AmountType.Coin,
            newValue = response.balance.toBigDecimal().movePointLeft(blockchain.decimals())
        )

        transactionBuilder.unspentOutputs = response.unspentOutputs

        wallet.recentTransactions.forEach { recentTransaction ->
            if (response.recentTransactionsHashes.isEmpty()) { // case for Rosetta API, it lacks recent transactions
                if (response.unspentOutputs.isEmpty() ||
                    response.unspentOutputs.find {
                        it.transactionHash.toHexString()
                            .equals(recentTransaction.hash, ignoreCase = true)
                    } != null
                ) {
                    recentTransaction.status = TransactionStatus.Confirmed
                }
            } else { // case for APIs with recent transactions
                if (response.recentTransactionsHashes
                        .find { it.equals(recentTransaction.hash, true) } != null
                ) {
                    recentTransaction.status = TransactionStatus.Confirmed
                }
            }
        }
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) throw error
    }

    override suspend fun send(
        transactionData: TransactionData, signer: TransactionSigner,
    ): SimpleResult {
        val transactionHash = transactionBuilder.buildToSign(transactionData)
        val signerResponse = signer.sign(transactionHash, wallet.publicKey)
        return when (signerResponse) {
            is CompletionResult.Success -> {
                val transactionToSend = transactionBuilder.buildToSend(signerResponse.data)
                val sendResult = networkProvider.sendTransaction(transactionToSend)

                if (sendResult is SimpleResult.Success) {
                    transactionData.hash = transactionHash.toHexString()
                    wallet.addOutgoingTransaction(transactionData)
                }
                sendResult
            }

            is CompletionResult.Failure -> SimpleResult.fromTangemSdkError(signerResponse.error)
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        val size = transactionBuilder.getEstimateSize(
            TransactionData(
                amount = amount,
                fee = null,
                sourceAddress = wallet.address,
                destinationAddress = destination
            )
        )
        val fee = (MINIMAL_FEE + COST_OF_KB * size).toBigDecimal().setScale(blockchain.decimals(), RoundingMode.UP)
        return Result.Success(TransactionFee.Single(Amount(amount, fee)))
    }

    companion object {

        private const val MINIMAL_FEE = 0.155381
        private const val COST_OF_KB = 0.000044
    }
}