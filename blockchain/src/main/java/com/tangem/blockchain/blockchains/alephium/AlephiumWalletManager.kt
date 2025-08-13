package com.tangem.blockchain.blockchains.alephium

import com.tangem.blockchain.blockchains.alephium.AlephiumTransactionBuilder.Companion.MAX_INPUT_COUNT
import com.tangem.blockchain.blockchains.alephium.network.AlephiumNetworkProvider
import com.tangem.blockchain.blockchains.alephium.source.dustUtxoAmount
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.common.transaction.getMinimumRequiredUTXOsToSend
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.fold
import com.tangem.blockchain.extensions.map
import com.tangem.blockchain.extensions.successOr
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toCompressedPublicKey
import com.tangem.common.extensions.toHexString
import java.math.BigDecimal
import kotlin.math.max

internal class AlephiumWalletManager(
    wallet: Wallet,
    private val networkService: AlephiumNetworkProvider,
    private val transactionBuilder: AlephiumTransactionBuilder,
) : WalletManager(wallet),
    UtxoAmountLimitProvider,
    UtxoBlockchainManager {

    override val currentHost: String get() = networkService.baseUrl

    override val dustValue: BigDecimal = dustUtxoAmount.v.toBigDecimal().movePointLeft(wallet.blockchain.decimals())

    override val allowConsolidation: Boolean = true

    override suspend fun updateInternal() {
        val info = networkService.getInfo(wallet.address).successOr { return }
        var hasUnconfirmed = false
        val nowMillis = System.currentTimeMillis()
        val amount = info
            .utxos
            .filter {
                if (!it.isConfirmed) hasUnconfirmed = true
                it.isNotFromFuture(nowMillis)
            }
            .sumOf {
                it.amount.toBigDecimal()
            }
            .movePointLeft(wallet.blockchain.decimals())
        transactionBuilder.updateUnspentOutputs(info)
        if (!hasUnconfirmed) wallet.recentTransactions.forEach { it.status = TransactionStatus.Confirmed }
        wallet.setCoinValue(amount)
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        val unsignedTransaction = transactionBuilder
            .buildToSign(transactionData)
            .successOr { return it }
        val hex = unsignedTransaction.id.bytes()

        return when (val signerResult = signer.sign(hex, wallet.publicKey)) {
            is CompletionResult.Success -> {
                val unsignedTx = transactionBuilder.serializeUnsignedTransaction(unsignedTransaction)
                    .toByteArray().toHexString()
                val signature = signerResult.data.toHexString()

                return when (val sendResult = networkService.submitTx(unsignedTx, signature)) {
                    is Result.Failure -> return Result.Failure(sendResult.error)
                    is Result.Success -> {
                        transactionData.hash = sendResult.data.txId
                        wallet.addOutgoingTransaction(transactionData)
                        Result.Success(TransactionSendResult(transactionData.hash ?: ""))
                    }
                }
            }
            is CompletionResult.Failure -> Result.fromTangemSdkError(signerResult.error)
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        var gasPrice = networkService.getFee(
            amount = dustValue.movePointRight(wallet.blockchain.decimals()),
            destination = destination,
            publicKey = wallet.publicKey.blockchainKey.toCompressedPublicKey().toHexString(),
        )
            .map { it.gasPrice }
            .successOr { return it }
        var gasAmount = calculateGasAmount(requireNotNull(amount.value).movePointRight(wallet.blockchain.decimals()))
            .successOr { return it }
        val fee = Fee.Alephium(amount, gasPrice, gasAmount)

        val unsignedTransaction = transactionBuilder
            .buildToSign(
                destinationAddress = destination,
                amount = amount,
                fee = fee,
            ).fold(
                success = { Result.Success(it) },
                failure = { error ->
                    when (error) {
                        // If the total amount (including the fee) is greater than the available balance,
                        // adjust the difference to cover the fee.
                        // https://github.com/alephium/alephium/blob/master/flow/src/main/scala/org/alephium/flow/core/UtxoSelectionAlgo.scala#L284
                        is BlockchainSdkError.Alephium.NotEnoughBalance -> transactionBuilder.buildToSign(
                            destinationAddress = destination,
                            amount = Amount(
                                amount = amount,
                                value = amount.value - (error.gotSum - error.expectedAmount).movePointLeft(
                                    amount.decimals,
                                ),
                            ),
                            fee = fee,
                        )
                        else -> Result.Failure(error)
                    }
                },
            )
            .successOr { return it }

        gasPrice = unsignedTransaction.gasPrice.value.v.toBigDecimal()
        gasAmount = unsignedTransaction.gasAmount.value.toBigDecimal()

        val feeAmount = (gasPrice * gasAmount).movePointLeft(wallet.blockchain.decimals())
        val feeModel = Fee.Alephium(
            amount = Amount(feeAmount, wallet.blockchain),
            gasPrice = gasPrice,
            gasAmount = gasAmount,
        )
        return Result.Success(TransactionFee.Single(feeModel))
    }

    private fun calculateGasAmount(amount: BigDecimal): Result<BigDecimal> {
        val unspentsToSpend = transactionBuilder.getMaxUnspentsToSpend()

        val inputs = getMinimumRequiredUTXOsToSend(
            unspentOutputs = unspentsToSpend,
            transactionAmount = amount,
            transactionFeeAmount = BigDecimal.ZERO,
            dustValue = dustUtxoAmount.v.toBigDecimal(),
            unspentToAmount = { it.output.amount.v.toBigDecimal() },
        ).successOr { return it }

        val inputsLength = inputs.size
        val inputGas = INPUT_BASE_GAS * inputsLength
        val outputGas = OUTPUT_BASE_GAS * 2
        val txGas = inputGas + outputGas + BASE_GAS + P2PK_UNLOCK_GAS
        val gasAmount = max(MINIMAL_GAS, txGas).toBigDecimal()
        return Result.Success(gasAmount)
    }

    override fun checkUtxoAmountLimit(amount: BigDecimal, fee: BigDecimal): UtxoAmountLimit {
        val fullAmount = transactionBuilder.getMaxUnspentsToSpendAmount()
            .movePointLeft(wallet.blockchain.decimals())
        val amountLimit = UtxoAmountLimit(
            limit = MAX_INPUT_COUNT.toBigDecimal(),
            availableToSpend = fullAmount,
            availableToSend = null,
        )
        val change = fullAmount - (amount + fee)

        // unspentsToSpend not enough to cover transaction amount
        return if (change < BigDecimal.ZERO || change > BigDecimal.ZERO && change < dustValue) {
            amountLimit.copy(availableToSend = amount + change - dustValue)
        } else {
            amountLimit
        }
    }

    companion object {
        private const val INPUT_BASE_GAS = 2000L
        private const val OUTPUT_BASE_GAS = 4500L
        private const val BASE_GAS = 1000L
        private const val P2PK_UNLOCK_GAS = 2060L
        private const val MINIMAL_GAS = 20000L
    }
}