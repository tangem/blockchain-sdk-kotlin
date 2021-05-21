package com.tangem.blockchain.blockchains.bitcoin

import android.util.Log
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressInfo
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinTransaction
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.sum
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.bitcoinj.core.TransactionInput
import java.math.BigDecimal

open class BitcoinWalletManager(
        wallet: Wallet,
        protected val transactionBuilder: BitcoinTransactionBuilder,
        private val networkProvider: BitcoinNetworkProvider
) : WalletManager(wallet), TransactionSender, TransactionPusher, SignatureCountValidator {

    protected val blockchain = wallet.blockchain
    open val minimalFeePerKb = 0.0001.toBigDecimal()
    open val minimalFee = 0.00001.toBigDecimal()
    private val cachedTransactions = mutableMapOf<String, BitcoinTransaction>()

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
            cachedTransactions.clear()
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

    private fun updateWallet(response: BitcoinAddressInfo) {
        Log.d(this::class.java.simpleName, "Balance is ${response.balance}")
        wallet.amounts[AmountType.Coin]?.value = response.balance
        transactionBuilder.unspentOutputs = response.unspentOutputs
        if (response.recentTransactions.isNotEmpty()) {
            updateRecentTransactionsBasic(response.recentTransactions)
        } else {
            when (response.hasUnconfirmed) {
                true -> wallet.addTransactionDummy()
                false -> wallet.recentTransactions.clear()
            }
        }
    }

    private fun updateError(error: Throwable?) {
        Log.e(this::class.java.simpleName, error?.message ?: "")
        if (error != null) throw error
    }

    override suspend fun send(
            transactionData: TransactionData,
            signer: TransactionSigner
    ) = send(transactionData, signer, null)

    protected suspend fun send(
            transactionData: TransactionData,
            signer: TransactionSigner,
            sequence: Long?
    ): SimpleResult {
        when (val buildTransactionResult = transactionBuilder.buildToSign(transactionData, sequence)) {
            is Result.Failure -> return SimpleResult.Failure(buildTransactionResult.error)
            is Result.Success -> {
                val signerResult = signer.sign(
                        buildTransactionResult.data,
                        wallet.cardId, walletPublicKey = wallet.publicKey
                )
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
                    is CompletionResult.Failure -> SimpleResult.failure(signerResult.error)
                }
            }
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        try {
            when (val feeResult = networkProvider.getFee()) {
                is Result.Failure -> return feeResult
                is Result.Success -> {
                    val feeValue = BigDecimal.ONE.movePointLeft(blockchain.decimals())
                    amount.value = amount.value!! - feeValue
                    val sizeResult = transactionBuilder.getEstimateSize(
                            TransactionData(amount, Amount(amount, feeValue), wallet.address, destination)
                    )
                    return when (sizeResult) {
                        is Result.Failure -> sizeResult
                        is Result.Success -> {
                            val transactionSize = sizeResult.data.toBigDecimal()
                            val minFee = feeResult.data.minimalPerKb.calculateFee(transactionSize)
                            val normalFee = feeResult.data.normalPerKb.calculateFee(transactionSize)
                            val priorityFee = feeResult.data.priorityPerKb.calculateFee(transactionSize)
                            val fees = listOf(Amount(minFee, blockchain),
                                    Amount(normalFee, blockchain),
                                    Amount(priorityFee, blockchain)
                            )
                            Result.Success(fees)
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            return Result.Failure(exception)
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

    private fun BigDecimal.calculateFee(transactionSize: BigDecimal): BigDecimal {
        val feePerKb = maxOf(this, minimalFeePerKb)
        val bytesInKb = BigDecimal(1024)
        val calculatedFee = feePerKb.divide(bytesInKb).multiply(transactionSize)
                .setScale(8, BigDecimal.ROUND_DOWN)
        return maxOf(calculatedFee, minimalFee)
    }

    companion object {
        const val DEFAULT_MINIMAL_FEE_PER_KB = 0.00001024
    }

    override suspend fun isPushAvailable(transactionHash: String): Result<Boolean> {
        if (!networkProvider.supportsRbf) return Result.Success(false)

        val transaction = when (val result = getTransaction(transactionHash)) {
            is Result.Success -> result.data
            is Result.Failure -> return result
        }
        val notRbfInput = transaction.inputs
                .find { it.sequence >= TransactionInput.NO_SEQUENCE - 1 }
        val walletAddresses = wallet.addresses.map { it.value }
        val otherAccountInput =
                transaction.inputs.find { !walletAddresses.contains(it.sender) }

        return Result.Success(
                notRbfInput == null && otherAccountInput == null && !transaction.isConfirmed
        )
    }

    override suspend fun getTransactionData(transactionHash: String) =
            when (val result = getTransaction(transactionHash)) {
                is Result.Success -> Result.Success(result.data.toTransactionData())
                is Result.Failure -> result
            }

    override suspend fun getPushFee(
            transactionHash: String,
            amount: Amount,
            destination: String
    ): Result<List<Amount>> {
        val transaction = when (val result = getTransaction(transactionHash)) {
            is Result.Success -> result.data
            is Result.Failure -> return result
        }
        val unspentOutputs = transactionBuilder.unspentOutputs ?: emptyList()

        val pushUnspentOutputs = transaction.inputs.map { it.unspentOutput }
        transactionBuilder.unspentOutputs =
                unspentOutputs.filterOutTransaction(transactionHash) + pushUnspentOutputs

        val fee = getFee(amount, destination)
        transactionBuilder.unspentOutputs = unspentOutputs
        return fee
    }

    override suspend fun push(
            transactionHash: String,
            newTransactionData: TransactionData,
            signer: TransactionSigner
    ): SimpleResult {
        val transaction = when (val result = getTransaction(transactionHash)) {
            is Result.Success -> result.data
            is Result.Failure -> return SimpleResult.Failure(result.error)
        }
        val oldFee = transaction.toTransactionData().fee?.value ?: 0.toBigDecimal()
        val newFee = newTransactionData.fee?.value ?: 0.toBigDecimal()
        if (newFee <= oldFee) {
            return SimpleResult.Failure(Exception("New fee should be greater than the old"))
        }

        transactionBuilder.unspentOutputs = transaction.inputs.map { it.unspentOutput }.plus(
                        transactionBuilder.unspentOutputs?.filterOutTransaction(transactionHash)
                        ?: emptyList()
                )
        val sequence = transaction.inputs.map { it.sequence }.maxOrNull()
                ?: return SimpleResult.Failure(Exception("Transaction inputs are absent"))

        return send(newTransactionData, signer, sequence + 1)
    }

    private suspend fun getTransaction(transactionHash: String): Result<BitcoinTransaction> {
        val transaction = cachedTransactions[transactionHash]
                ?: when (val result = networkProvider.getTransaction(transactionHash)) {
                    is Result.Success -> result.data
                            .also { cachedTransactions[it.hash] = it }
                    is Result.Failure -> return result
                }
        return Result.Success(transaction)
    }

    private fun BitcoinTransaction.toTransactionData(): TransactionData {
        val inputsTotal = inputs.map { it.unspentOutput.amount }.sum()

        val walletAddresses = wallet.addresses.map { it.value }
        val (changeOutputs, sentOutputs) =
                outputs.partition { walletAddresses.contains(it.recipient) }
        val changeOutput = changeOutputs.firstOrNull()
        val sentOutput = sentOutputs.first()

        val changeValue = changeOutput?.amount ?: 0.toBigDecimal()
        val sentValue = sentOutput.amount
        val feeValue = inputsTotal - sentValue - changeValue

        return TransactionData(
                amount = Amount(sentValue, blockchain),
                fee = Amount(feeValue, blockchain),
                sourceAddress = inputs.first().sender,
                destinationAddress = sentOutput.recipient,
                date = time,
                hash = hash
        )
    }

    private fun List<BitcoinUnspentOutput>.filterOutTransaction(transactionHash: String) =
            this.filter { !it.transactionHash.contentEquals(transactionHash.hexToBytes()) }
}