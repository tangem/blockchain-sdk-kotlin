package com.tangem.blockchain.blockchains.bitcoin

import android.util.Log
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressInfo
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.BasicTransactionData
import com.tangem.blockchain.common.BlockchainError
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.SignatureCountValidator
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.txhistory.DefaultTransactionHistoryProvider
import com.tangem.blockchain.common.txhistory.TransactionHistoryProvider
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal

open class BitcoinWalletManager(
    wallet: Wallet,
    transactionHistoryProvider: TransactionHistoryProvider = DefaultTransactionHistoryProvider,
    protected val transactionBuilder: BitcoinTransactionBuilder,
    private val networkProvider: BitcoinNetworkProvider,
) : WalletManager(wallet, transactionHistoryProvider = transactionHistoryProvider),
    TransactionSender,
    SignatureCountValidator {

    protected val blockchain = wallet.blockchain

    open val minimalFeePerKb = DEFAULT_MINIMAL_FEE_PER_KB.toBigDecimal()
    open val minimalFee = 0.000001.toBigDecimal()

    override val currentHost: String
        get() = networkProvider.baseUrl

    override suspend fun update() {
        coroutineScope {
            val addressInfos = mutableListOf<BitcoinAddressInfo>()
            val responsesDeferred =
                wallet.addresses.map { async { networkProvider.getInfo(it.value) } }

            responsesDeferred.forEach {
                when (val response = it.await()) {
                    is Result.Success -> addressInfos.add(response.data)
                    is Result.Failure -> {
                        updateError(response.error)
                        return@coroutineScope
                    }
                }
            }
            updateWallet(addressInfos.merge())
        }
    }

    private fun List<BitcoinAddressInfo>.merge(): BitcoinAddressInfo {
        var balance = BigDecimal.ZERO
        val unspentOutputs = mutableListOf<BitcoinUnspentOutput>()
        val recentTransactions = mutableListOf<BasicTransactionData>()
        var hasUnconfirmed: Boolean? = false

        this.forEach {
            balance += it.balance
            unspentOutputs.addAll(it.unspentOutputs)
            recentTransactions.addAll(it.recentTransactions)
            hasUnconfirmed = if (hasUnconfirmed == null || it.hasUnconfirmed == null) {
                null
            } else {
                hasUnconfirmed!! || it.hasUnconfirmed
            }
        }

        // merge same transaction on different addresses
        val transactionsByHash = mutableMapOf<String, List<BasicTransactionData>>()
        recentTransactions.forEach { transaction ->
            val sameHashTransactions = recentTransactions.filter { it.hash == transaction.hash }
            transactionsByHash[transaction.hash] = sameHashTransactions
        }
        val finalTransactions = transactionsByHash.map {
            BasicTransactionData(
                balanceDif = it.value.sumOf { transaction -> transaction.balanceDif },
                hash = it.value[0].hash,
                date = it.value[0].date,
                isConfirmed = it.value[0].isConfirmed
            )
        }
        return BitcoinAddressInfo(balance, unspentOutputs, finalTransactions, hasUnconfirmed)
    }

    internal fun updateWallet(response: BitcoinAddressInfo) {
        Log.d(this::class.java.simpleName, "Balance is ${response.balance}")

        wallet.changeAmountValue(AmountType.Coin, response.balance)
        transactionBuilder.unspentOutputs = response.unspentOutputs
        outputsCount = response.unspentOutputs.size

        if (response.recentTransactions.isNotEmpty()) {
            updateRecentTransactionsBasic(response.recentTransactions)
        } else {
            when (response.hasUnconfirmed) {
                true -> wallet.addTransactionDummy()
                else -> wallet.recentTransactions.clear()
            }
        }
    }

    internal fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        (error as? BlockchainSdkError)?.let { throw it }
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        when (val buildTransactionResult = transactionBuilder.buildToSign(transactionData)) {
            is Result.Failure -> return SimpleResult.Failure(buildTransactionResult.error)
            is Result.Success -> {
                val signerResult = signer.sign(buildTransactionResult.data, wallet.publicKey)
                return when (signerResult) {
                    is CompletionResult.Success -> {
                        val transactionToSend = transactionBuilder.buildToSend(
                            signerResult.data.reduce { acc, bytes -> acc + bytes }
                        )
                        val sendResult = networkProvider.sendTransaction(transactionToSend.toHexString())

                        if (sendResult is SimpleResult.Success) {
                            transactionData.hash = transactionBuilder.getTransactionHash().toHexString()
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
        try {
            when (val feeResult = getBitcoinFeePerKb()) {
                is Result.Failure -> return feeResult
                is Result.Success -> {
                    val feeValue = BigDecimal.ONE.movePointLeft(blockchain.decimals())

                    val newAmount =  amount.copy(value = amount.value!! - feeValue)

                    val sizeResult = transactionBuilder.getEstimateSize(
                        TransactionData(
                            amount = newAmount,
                            fee = Fee.Common(Amount(newAmount, feeValue)),
                            sourceAddress = wallet.address,
                            destinationAddress = destination
                        )
                    )

                    return when (sizeResult) {
                        is Result.Failure -> sizeResult
                        is Result.Success -> {
                            val transactionSize = sizeResult.data.toBigDecimal()
                            val minFee = feeResult.data.minimalPerKb.calculateFee(transactionSize)
                            val normalFee = feeResult.data.normalPerKb.calculateFee(transactionSize)
                            val priorityFee = feeResult.data.priorityPerKb.calculateFee(transactionSize)
                            val fees = TransactionFee.Choosable(
                                minimum = Fee.Common(Amount(minFee, blockchain)),
                                normal = Fee.Common(Amount(normalFee, blockchain)),
                                priority = Fee.Common(Amount(priorityFee, blockchain))
                            )
                            Result.Success(fees)
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            return Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun validateSignatureCount(signedHashes: Int): SimpleResult {
        return when (val result = networkProvider.getSignatureCount(wallet.address)) {
            is Result.Success -> if (result.data == signedHashes) {
                SimpleResult.Success
            } else {
                SimpleResult.Failure(BlockchainSdkError.SignatureCountNotMatched)
            }
            is Result.Failure -> SimpleResult.Failure(result.error)
        }
    }

    protected open suspend fun getBitcoinFeePerKb(): Result<BitcoinFee> = networkProvider.getFee()

    protected open fun BigDecimal.calculateFee(transactionSize: BigDecimal): BigDecimal {
        val feePerKb = maxOf(a = this, b = minimalFeePerKb)
        val calculatedFee = feePerKb
            .divide(BigDecimal(BYTES_IN_KB))
            .multiply(transactionSize)
            .setScale(blockchain.decimals(), BigDecimal.ROUND_DOWN)

        return maxOf(calculatedFee, minimalFee)
    }

    companion object {
        const val DEFAULT_MINIMAL_FEE_PER_KB = 0.00001024

        private const val BYTES_IN_KB = 1024
    }
}
