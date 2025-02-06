package com.tangem.blockchain.blockchains.kaspa

import com.tangem.blockchain.blockchains.kaspa.network.KaspaFeeEstimation
import com.tangem.blockchain.blockchains.kaspa.network.KaspaNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.common.CompletionResult
import java.math.BigDecimal
import java.math.BigInteger

class KaspaFeesCalculator(
    private val blockchain: Blockchain,
    private val transactionBuilder: KaspaTransactionBuilder,
    private val networkProvider: KaspaNetworkProvider,
    private val publicKey: Wallet.PublicKey,
) {
    private val dummySigner = DummySigner()

    suspend fun estimateFee(
        amount: Amount,
        transactionData: TransactionData,
        dustValue: BigDecimal,
    ): Result<KaspaFeeEstimation> {
        val buildTransactionResult = when (amount.type) {
            is AmountType.Coin -> transactionBuilder.buildToSign(transactionData, dustValue)
            is AmountType.Token -> transactionBuilder.buildToSignKRC20Commit(
                transactionData = transactionData,
                dustValue = dustValue,
                includeFee = false,
            ).let {
                when (it) {
                    is Result.Failure -> it
                    is Result.Success -> Result.Success(it.data.transaction)
                }
            }
            else -> error("unknown amount type for fee estimation")
        }
        return when (buildTransactionResult) {
            is Result.Failure -> return buildTransactionResult
            is Result.Success -> callEstimateFee(buildTransactionResult.data)
        }
    }

    fun toTransactionFee(feeEstimation: KaspaFeeEstimation, amountType: AmountType): TransactionFee.Choosable {
        val allBuckets = (listOf(feeEstimation.priorityBucket) + feeEstimation.normalBuckets + feeEstimation.lowBuckets)
            .sortedByDescending { it.feeRate }
        val feeMass = feeEstimation.mass.toBigInteger()
        return TransactionFee.Choosable(
            priority = toFee(allBuckets[0].feeRate, feeMass, amountType),
            normal = toFee(allBuckets[1].feeRate, feeMass, amountType),
            minimum = toFee(allBuckets[2].feeRate, feeMass, amountType),
        )
    }

    private suspend fun callEstimateFee(transaction: KaspaTransaction): Result<KaspaFeeEstimation> {
        val hashesToSign = transactionBuilder.getHashesForSign(transaction)
        return when (val signerResult = dummySigner.sign(hashesToSign, publicKey)) {
            is CompletionResult.Success -> {
                val transactionToSend = transactionBuilder.buildToSend(
                    signatures = signerResult.data.reduce { acc, bytes -> acc + bytes },
                    transaction = transaction,
                )
                networkProvider.calculateFee(transactionToSend.transaction)
            }
            is CompletionResult.Failure -> Result.fromTangemSdkError(signerResult.error)
        }
    }

    private fun toFee(feeRate: Long, mass: BigInteger, type: AmountType): Fee.Kaspa {
        val feeRateInteger = feeRate.toBigInteger()
        val resultMass = if (type is AmountType.Token) {
            mass + REVEAL_TRANSACTION_MASS
        } else {
            mass
        }
        val value = (resultMass * feeRateInteger).toBigDecimal().movePointLeft(blockchain.decimals())
        return Fee.Kaspa(
            amount = Amount(
                value = value,
                blockchain = blockchain,
            ),
            mass = mass,
            feeRate = feeRateInteger,
            revealTransactionFee = type.takeIf { it is AmountType.Token }.let {
                Amount(
                    value = (REVEAL_TRANSACTION_MASS * feeRateInteger).toBigDecimal().movePointLeft(
                        blockchain.decimals(),
                    ),
                    blockchain = blockchain,
                )
            },
        )
    }

    private companion object {
        private val REVEAL_TRANSACTION_MASS: BigInteger = 4100.toBigInteger()
    }
}