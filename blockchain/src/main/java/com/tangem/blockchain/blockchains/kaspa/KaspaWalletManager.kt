package com.tangem.blockchain.blockchains.kaspa

import android.util.Log
import com.tangem.blockchain.blockchains.kaspa.network.KaspaInfoResponse
import com.tangem.blockchain.blockchains.kaspa.network.KaspaNetworkProvider
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.BlockchainError
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import java.math.BigDecimal

class KaspaWalletManager(
    wallet: Wallet,
    private val transactionBuilder: KaspaTransactionBuilder,
    private val networkProvider: KaspaNetworkProvider,
) : WalletManager(wallet), TransactionSender {

    override val currentHost: String
        get() = networkProvider.baseUrl

    private val blockchain = wallet.blockchain
    override val dustValue: BigDecimal = FEE_PER_UNSPENT_OUTPUT.toBigDecimal()

    override suspend fun updateInternal() {
        when (val response = networkProvider.getInfo(wallet.address)) {
            is Result.Success -> updateWallet(response.data)
            is Result.Failure -> updateError(response.error)
        }
    }

    private fun updateWallet(response: KaspaInfoResponse) {
        Log.d(this::class.java.simpleName, "Balance is ${response.balance}")
        if (response.balance != wallet.amounts[AmountType.Coin]?.value) {
            wallet.recentTransactions.clear()
        }
        wallet.changeAmountValue(AmountType.Coin, response.balance)
        transactionBuilder.unspentOutputs = response.unspentOutputs
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) throw error
    }

    override suspend fun send(
        transactionData: TransactionData, signer: TransactionSigner,
    ): SimpleResult {
        when (val buildTransactionResult = transactionBuilder.buildToSign(transactionData)) {
            is Result.Failure -> return SimpleResult.Failure(buildTransactionResult.error)
            is Result.Success -> {
                val signerResult = signer.sign(buildTransactionResult.data, wallet.publicKey)
                return when (signerResult) {
                    is CompletionResult.Success -> {
                        val transactionToSend = transactionBuilder.buildToSend(
                            signerResult.data.reduce { acc, bytes -> acc + bytes }
                        )
                        val sendResult = networkProvider.sendTransaction(transactionToSend)

                        if (sendResult is SimpleResult.Success) {
                            wallet.addOutgoingTransaction(transactionData)
                        }
                        sendResult
                    }
                    is CompletionResult.Failure -> SimpleResult.fromTangemSdkError(signerResult.error)
                }
            }
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        val unspentOutputCount = transactionBuilder.getUnspentsToSpendCount()

        return if (unspentOutputCount == 0) {
            Result.Failure(Exception("No unspent outputs found").toBlockchainSdkError()) // shouldn't happen
        } else {
            val fee = FEE_PER_UNSPENT_OUTPUT.toBigDecimal().multiply(unspentOutputCount.toBigDecimal())
            Result.Success(TransactionFee.Single(Fee.Common(Amount(fee, blockchain))))
        }
    }

    companion object {
        const val FEE_PER_UNSPENT_OUTPUT = 0.0001
    }
}
