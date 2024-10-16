package com.tangem.blockchain.blockchains.kaspa

import android.util.Log
import com.tangem.blockchain.blockchains.kaspa.network.KaspaFeeBucketResponse
import com.tangem.blockchain.blockchains.kaspa.network.KaspaInfoResponse
import com.tangem.blockchain.blockchains.kaspa.network.KaspaNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.common.CompletionResult
import java.math.BigDecimal
import java.math.BigInteger

class KaspaWalletManager(
    wallet: Wallet,
    private val transactionBuilder: KaspaTransactionBuilder,
    private val networkProvider: KaspaNetworkProvider,
) : WalletManager(wallet), UtxoAmountLimitProvider, UtxoBlockchainManager {

    override val currentHost: String
        get() = networkProvider.baseUrl

    private val blockchain = wallet.blockchain
    override val dustValue: BigDecimal = BigDecimal("0.2")

    private val dummySigner = DummySigner()

    override val allowConsolidation: Boolean = true

    override suspend fun updateInternal() {
        when (val response = networkProvider.getInfo(wallet.address)) {
            is Result.Success -> updateWallet(response.data)
            is Result.Failure -> updateError(response.error)
        }
    }

    private fun updateWallet(response: KaspaInfoResponse) {
        Log.d(this::class.java.simpleName, "Balance is ${response.balance}")
        if (response.balance != wallet.amounts[AmountType.Coin]?.value) {
            // assume outgoing transaction has been finalized if balance has changed
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
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        when (val buildTransactionResult = transactionBuilder.buildToSign(transactionData)) {
            is Result.Failure -> return buildTransactionResult
            is Result.Success -> {
                return when (val signerResult = signer.sign(buildTransactionResult.data, wallet.publicKey)) {
                    is CompletionResult.Success -> {
                        val transactionToSend = transactionBuilder.buildToSend(
                            signerResult.data.reduce { acc, bytes -> acc + bytes },
                        )
                        when (val sendResult = networkProvider.sendTransaction(transactionToSend)) {
                            is Result.Failure -> sendResult
                            is Result.Success -> {
                                val hash = sendResult.data
                                transactionData.hash = hash
                                wallet.addOutgoingTransaction(transactionData)
                                Result.Success(TransactionSendResult(hash ?: ""))
                            }
                        }
                    }
                    is CompletionResult.Failure -> Result.fromTangemSdkError(signerResult.error)
                }
            }
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        val unspentOutputCount = transactionBuilder.getUnspentsToSpendCount()

        return if (unspentOutputCount == 0) {
            Result.Failure(Exception("No unspent outputs found").toBlockchainSdkError()) // shouldn't happen
        } else {
            val source = wallet.address

            val transactionData = TransactionData.Uncompiled(
                sourceAddress = source,
                destinationAddress = destination,
                amount = amount,
                fee = null,
            )

            when (val buildTransactionResult = transactionBuilder.buildToSign(transactionData)) {
                is Result.Failure -> return buildTransactionResult
                is Result.Success -> {
                    return when (val signerResult = dummySigner.sign(buildTransactionResult.data, wallet.publicKey)) {
                        is CompletionResult.Success -> {
                            val transactionToSend = transactionBuilder.buildToSend(
                                signerResult.data.reduce { acc, bytes -> acc + bytes },
                            )
                            when (val sendResult = networkProvider.calculateFee(transactionToSend.transaction)) {
                                is Result.Failure -> sendResult
                                is Result.Success -> {
                                    val data = sendResult.data
                                    val mass = BigInteger.valueOf(data.mass)

                                    val allBuckets = (
                                        listOf(data.priorityBucket) +
                                            data.normalBuckets +
                                            data.lowBuckets
                                        ).sortedByDescending { it.feeRate }

                                    Result.Success(
                                        TransactionFee.Choosable(
                                            priority = allBuckets[0].toFee(mass),
                                            normal = allBuckets[1].toFee(mass),
                                            minimum = allBuckets[2].toFee(mass),
                                        ),
                                    )
                                }
                            }
                        }
                        is CompletionResult.Failure -> Result.fromTangemSdkError(signerResult.error)
                    }
                }
            }
        }
    }

    override fun checkUtxoAmountLimit(amount: BigDecimal, fee: BigDecimal): UtxoAmountLimit? {
        val unspents = transactionBuilder.getUnspentsToSpend()
        val change = transactionBuilder.calculateChange(amount, fee, unspents)
        return if (change < BigDecimal.ZERO) { // unspentsToSpend not enough to cover transaction amount
            UtxoAmountLimit(
                KaspaTransactionBuilder.MAX_INPUT_COUNT.toBigDecimal(),
                amount + change,
            )
        } else {
            null
        }
    }

    private fun KaspaFeeBucketResponse.toFee(mass: BigInteger): Fee.Kaspa {
        val feeRate = feeRate.toBigInteger()
        val value = (mass * feeRate).toBigDecimal().movePointLeft(blockchain.decimals())
        return Fee.Kaspa(
            amount = Amount(
                value = value,
                blockchain = blockchain,
            ),
            mass = mass,
            feeRate = feeRate,
        )
    }
}
