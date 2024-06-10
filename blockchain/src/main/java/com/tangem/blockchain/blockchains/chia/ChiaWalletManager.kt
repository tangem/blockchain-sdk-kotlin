package com.tangem.blockchain.blockchains.chia

import android.util.Log
import com.tangem.blockchain.blockchains.chia.network.ChiaCoin
import com.tangem.blockchain.blockchains.chia.network.ChiaNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import java.math.BigDecimal

class ChiaWalletManager(
    wallet: Wallet,
    private val transactionBuilder: ChiaTransactionBuilder,
    private val networkProvider: ChiaNetworkProvider,
) : WalletManager(wallet), TransactionSender, UtxoAmountLimitProvider {

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
            // assume outgoing transaction has been finalized if balance has changed
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
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        when (val buildTransactionResult = transactionBuilder.buildToSign(transactionData)) {
            is Result.Failure -> return Result.Failure(buildTransactionResult.error)
            is Result.Success -> {
                return when (val signerResult = signer.sign(buildTransactionResult.data, wallet.publicKey)) {
                    is CompletionResult.Success -> {
                        val transactionToSend = transactionBuilder.buildToSend(signerResult.data)
                        when (val sendResult = networkProvider.sendTransaction(transactionToSend)) {
                            is SimpleResult.Success -> {
                                transactionData.hash = transactionToSend.spendBundle.aggregatedSignature
                                wallet.addOutgoingTransaction(transactionData)
                                Result.Success(TransactionSendResult(transactionData.hash ?: ""))
                            }
                            is SimpleResult.Failure -> Result.Failure(sendResult.error)
                        }
                    }
                    is CompletionResult.Failure -> Result.fromTangemSdkError(signerResult.error)
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
                        minimum = Fee.Common(Amount(result.data.minimalFee, blockchain)),
                        normal = Fee.Common(Amount(result.data.normalFee, blockchain)),
                        priority = Fee.Common(Amount(result.data.priorityFee, blockchain)),
                    ),
                )
            }
            is Result.Failure -> result
        }
    }

    override fun checkUtxoAmountLimit(amount: BigDecimal, fee: BigDecimal): UtxoAmountLimit? {
        val unspents = transactionBuilder.getUnspentsToSpend()
        val change = transactionBuilder.calculateChange(amount, fee, unspents)
        val changeDecimal = change.toBigDecimal().movePointLeft(blockchain.decimals())
        return if (changeDecimal < BigDecimal.ZERO) { // unspentsToSpend not enough to cover transaction amount
            UtxoAmountLimit(
                maxLimit = ChiaTransactionBuilder.MAX_INPUT_COUNT.toBigDecimal(),
                maxAmount = amount + changeDecimal,
            )
        } else {
            null
        }
    }
}