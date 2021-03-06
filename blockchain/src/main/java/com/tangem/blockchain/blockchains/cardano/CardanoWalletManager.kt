package com.tangem.blockchain.blockchains.cardano

import android.util.Log
import com.tangem.blockchain.blockchains.cardano.network.CardanoAddressResponse
import com.tangem.blockchain.blockchains.cardano.network.CardanoNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.commands.SignResponse
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import java.math.RoundingMode

class CardanoWalletManager(
        cardId: String,
        wallet: Wallet,
        private val transactionBuilder: CardanoTransactionBuilder,
        private val networkProvider: CardanoNetworkProvider
) : WalletManager(cardId, wallet), TransactionSender {
    init {
        dustValue = 1.toBigDecimal()
    }

    private val blockchain = wallet.blockchain

    override suspend fun update() {
        when (val response = networkProvider.getInfo(wallet.addresses.map { it.value }.toSet())) {
            is Result.Success -> updateWallet(response.data)
            is Result.Failure -> updateError(response.error)
        }
    }

    private fun updateWallet(response: CardanoAddressResponse) {
        Log.d(this::class.java.simpleName, "Balance is ${response.balance.toString()}")
        wallet.amounts[AmountType.Coin]?.value =
                response.balance.toBigDecimal().movePointLeft(blockchain.decimals())
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

    private fun updateError(error: Throwable?) {
        Log.e(this::class.java.simpleName, error?.message ?: "")
        if (error != null) throw error
    }

    override suspend fun send(
            transactionData: TransactionData, signer: TransactionSigner
    ): Result<SignResponse> {
        val transactionHash = transactionBuilder.buildToSign(transactionData)

        return when (val signerResponse = signer.sign(arrayOf(transactionHash), cardId)) {
            is CompletionResult.Success -> {
                val transactionToSend = transactionBuilder.buildToSend(signerResponse.data.signature)
                val sendResult = networkProvider.sendTransaction(transactionToSend)

                if (sendResult is SimpleResult.Success) {
                    transactionData.hash = transactionHash.toHexString()
                    wallet.addOutgoingTransaction(transactionData)
                }
                sendResult.toResultWithData(signerResponse.data)
            }
            is CompletionResult.Failure -> Result.failure(signerResponse.error)
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        val a = 0.155381
        val b = 0.000044
        val size = transactionBuilder.getEstimateSize(
                TransactionData(amount, null, wallet.address, destination)
        )
        val fee = (a + b * size).toBigDecimal().setScale(blockchain.decimals(), RoundingMode.UP)
        return Result.Success(listOf(Amount(amount, fee)))
    }
}