package com.tangem.blockchain.blockchains.bitcoin

import android.util.Log
import com.tangem.blockchain.blockchains.bitcoin.address.BitcoinAddressProvider
import com.tangem.blockchain.blockchains.bitcoin.messagesigning.BitcoinMessageSigner
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressInfo
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.bitcoin.psbt.BitcoinPsbtProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.AddressProvider
import com.tangem.blockchain.common.messagesigning.MessageSigner
import com.tangem.blockchain.common.psbt.PsbtProvider
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.transactionhistory.DefaultTransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
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
    private val feesCalculator: BitcoinFeesCalculator,
) : WalletManager(
    wallet = wallet,
    transactionHistoryProvider = transactionHistoryProvider,
),
    SignatureCountValidator,
    UtxoBlockchainManager {

    protected val blockchain = wallet.blockchain

    override val messageSigner: MessageSigner = BitcoinMessageSigner(wallet)
    override val psbtProvider: PsbtProvider = BitcoinPsbtProvider(wallet, networkProvider)
    override val addressProvider: AddressProvider = BitcoinAddressProvider(wallet)

    override val dustValue: BigDecimal = feesCalculator.minimalFeePerKb

    override val allowConsolidation: Boolean = true

    override val currentHost: String
        get() = networkProvider.baseUrl

    override suspend fun updateInternal() {
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
                isConfirmed = it.value[0].isConfirmed,
                destination = it.value[0].destination,
                source = it.value[0].source,
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

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        when (val buildTransactionResult = transactionBuilder.buildToSign(transactionData, dustValue)) {
            is Result.Failure -> return buildTransactionResult
            is Result.Success -> {
                return when (val signerResult = signer.sign(buildTransactionResult.data, wallet.publicKey)) {
                    is CompletionResult.Success -> {
                        val transactionToSend = transactionBuilder.buildToSend(
                            signerResult.data.reduce { acc, bytes -> acc + bytes },
                        )
                        val sendResult = networkProvider.sendTransaction(transactionToSend.toHexString())

                        return when (sendResult) {
                            is SimpleResult.Success -> {
                                val txHash = transactionBuilder.getTransactionHash().toHexString()
                                wallet.addOutgoingTransaction(transactionData = transactionData, txHash = txHash)
                                Result.Success(TransactionSendResult(txHash))
                            }
                            is SimpleResult.Failure -> return Result.Failure(sendResult.error)
                        }
                    }
                    is CompletionResult.Failure -> Result.fromTangemSdkError(signerResult.error)
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

                    val newAmount = amount.copy(value = amount.value!! - feeValue)

                    val sizeResult = transactionBuilder.getEstimateSize(
                        transactionData = TransactionData.Uncompiled(
                            amount = newAmount,
                            fee = Fee.Common(Amount(newAmount, feeValue)),
                            sourceAddress = wallet.address,
                            destinationAddress = destination,
                        ),
                        dustValue = dustValue,
                    )

                    return when (sizeResult) {
                        is Result.Failure -> sizeResult
                        is Result.Success -> {
                            val fees = feesCalculator.calculateFees(
                                sizeResult.data.toBigDecimal(),
                                feeResult.data,
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

    companion object {
        // Synced value with iOS
        const val DEFAULT_MINIMAL_FEE_PER_KB = 0.00001
    }
}