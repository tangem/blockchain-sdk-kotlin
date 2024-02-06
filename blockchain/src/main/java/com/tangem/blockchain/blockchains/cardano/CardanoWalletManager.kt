package com.tangem.blockchain.blockchains.cardano

import android.util.Log
import com.tangem.blockchain.blockchains.cardano.network.CardanoAddressResponse
import com.tangem.blockchain.blockchains.cardano.network.CardanoNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import java.math.BigDecimal

class CardanoWalletManager(
    wallet: Wallet,
    private val transactionBuilder: CardanoTransactionBuilder,
    private val networkProvider: CardanoNetworkProvider,
) : WalletManager(wallet), TransactionSender {

    override val dustValue: BigDecimal = BigDecimal.ONE
    private val blockchain = wallet.blockchain

    override val currentHost: String
        get() = networkProvider.baseUrl

    override suspend fun updateInternal() {
        when (val response = networkProvider.getInfo(wallet.addresses.map { it.value }.toSet())) {
            is Result.Success -> updateWallet(response.data)
            is Result.Failure -> updateError(response.error)
        }
    }

    private fun updateWallet(response: CardanoAddressResponse) {
        Log.d(this::class.java.simpleName, "Balance is ${response.balance}")

        wallet.changeAmountValue(
            amountType = AmountType.Coin,
            newValue = response.balance.toBigDecimal().movePointLeft(blockchain.decimals()),
        )

        transactionBuilder.update(response.unspentOutputs)

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
                val recentTx = response.recentTransactionsHashes.find { it.equals(recentTransaction.hash, true) }
                if (recentTx != null) {
                    recentTransaction.status = TransactionStatus.Confirmed
                }
            }
        }
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) throw error
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val transactionHash = transactionBuilder.buildForSign(transactionData)

        return when (val signatureResult = signer.sign(transactionHash, wallet.publicKey)) {
            is CompletionResult.Success -> {
                val signatureInfo = SignatureInfo(signatureResult.data, wallet.publicKey.blockchainKey)

                val transactionToSend = transactionBuilder.buildForSend(transactionData, signatureInfo)
                val sendResult = networkProvider.sendTransaction(transactionToSend)

                if (sendResult is SimpleResult.Success) {
                    transactionData.hash = transactionHash.toHexString()
                    wallet.addOutgoingTransaction(transactionData)
                }

                sendResult
            }

            is CompletionResult.Failure -> SimpleResult.fromTangemSdkError(signatureResult.error)
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        val dummyTransaction = TransactionData(
            amount = amount,
            fee = null,
            sourceAddress = wallet.address,
            destinationAddress = destination,
        )

        val feeValue = transactionBuilder.estimatedFee(dummyTransaction)

        val fee = Fee.Common(
            Amount(
                value = feeValue.movePointLeft(blockchain.decimals()),
                blockchain = wallet.blockchain,
                type = AmountType.Coin,
            ),
        )

        return Result.Success(TransactionFee.Single(fee))
    }
}
