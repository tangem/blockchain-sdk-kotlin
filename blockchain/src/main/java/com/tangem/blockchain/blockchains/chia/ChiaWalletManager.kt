package com.tangem.blockchain.blockchains.chia

import android.util.Log
import com.tangem.blockchain.blockchains.chia.network.ChiaCoin
import com.tangem.blockchain.blockchains.chia.network.ChiaNetworkProvider
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.BlockchainError
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import java.math.BigDecimal

class ChiaWalletManager(
    wallet: Wallet,
    private val transactionBuilder: ChiaTransactionBuilder,
    private val networkProvider: ChiaNetworkProvider,
) : WalletManager(wallet), TransactionSender {

    override val currentHost: String
        get() = networkProvider.baseUrl

    private val blockchain = wallet.blockchain
    private val puzzleHash = ChiaAddressService.getPuzzleHash(wallet.address).toHexString()

    override suspend fun updateInternal() {
        when (val response = networkProvider.getUnspents(puzzleHash)) {
            is Result.Success -> updateWallet(response.data)
            is Result.Failure -> updateError(response.error)
        }
    }

    private fun updateWallet(unspentCoins: List<ChiaCoin>) {
        val balance = unspentCoins.sumOf { it.amount }.toBigDecimal().movePointLeft(blockchain.decimals())
        Log.d(this::class.java.simpleName, "Balance is $balance")

        if (balance != wallet.amounts[AmountType.Coin]?.value) {
            wallet.recentTransactions.clear()
        }
        wallet.changeAmountValue(AmountType.Coin, balance)
        transactionBuilder.unspentCoins = unspentCoins
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
                        val transactionToSend = transactionBuilder.buildToSend(signerResult.data)
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
        val transactionCost = transactionBuilder.getTransactionCost(amount)

        return when (val result = networkProvider.getFeeEstimate(transactionCost)) {
            is Result.Success -> {
                Result.Success(
                    TransactionFee.Choosable(
                        minimum = Fee.Common(Amount(BigDecimal.ZERO, blockchain)),
                        normal = Fee.Common(Amount(result.data.normalFee, blockchain)),
                        priority = Fee.Common(Amount(result.data.priorityFee, blockchain))
                    )
                )
            }
            is Result.Failure -> result
        }
    }
}
