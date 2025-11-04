package com.tangem.blockchain.blockchains.polkadot

import com.tangem.blockchain.blockchains.polkadot.extensions.makeEraFromBlockNumber
import com.tangem.blockchain.blockchains.polkadot.network.PolkadotNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.BlockchainSdkError.UnsupportedOperation
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.common.transaction.TransactionsSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.formatHex
import com.tangem.blockchain.extensions.successOr
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.hexToBytes
import io.emeraldpay.polkaj.tx.ExtrinsicContext
import io.emeraldpay.polkaj.types.Hash256
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.util.Calendar
import java.util.EnumSet

/**
[REDACTED_AUTHOR]
 */
internal class PolkadotWalletManager(
    wallet: Wallet,
    private val networkProvider: PolkadotNetworkProvider,
) : WalletManager(wallet), ExistentialDepositProvider {

    private lateinit var currentContext: ExtrinsicContext

    private val existentialDeposit: BigDecimal = when (wallet.blockchain) {
        Blockchain.Polkadot -> 0.01.toBigDecimal()
        Blockchain.PolkadotTestnet -> 0.01.toBigDecimal()
        Blockchain.Kusama -> 0.000003333.toBigDecimal()
        Blockchain.AlephZero, Blockchain.AlephZeroTestnet -> 0.0000000005.toBigDecimal()
        Blockchain.Joystream -> 0.026666656.toBigDecimal()
        Blockchain.Bittensor -> 0.0000005.toBigDecimal()
        Blockchain.EnergyWebX, Blockchain.EnergyWebXTestnet -> 0.000000000000000001.toBigDecimal()
        else -> error("${wallet.blockchain} isn't supported")
    }

    private val txBuilder = PolkadotTransactionBuilder(wallet.blockchain)

    override val currentHost: String
        get() = networkProvider.baseUrl

    override fun getExistentialDeposit() = existentialDeposit

    override suspend fun updateInternal() {
        val amount = networkProvider.getBalance(wallet.address).successOr {
            wallet.removeAllTokens()
            throw it.error as BlockchainSdkError
        }
        wallet.setCoinValue(amount)
        updateRecentTransactions()
    }

    @Suppress("MagicNumber")
    private fun updateRecentTransactions() {
        val currentTimeInMillis = Calendar.getInstance().timeInMillis
        val confirmedTxData = wallet.recentTransactions
            .filter {
                it.hash != null && it.date != null
            }
            .filter {
                val txTimeInMillis = it.date?.timeInMillis ?: currentTimeInMillis
                currentTimeInMillis - txTimeInMillis > 9999
            }.map {
                it.updateStatus(status = TransactionStatus.Confirmed) as TransactionData.Uncompiled
            }

        updateRecentTransactions(confirmedTxData)
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        currentContext = networkProvider.extrinsicContext(wallet.address).successOr { return it }
        runCatching { updateEra() }.onFailure {
            Result.Failure(
                BlockchainSdkError.CustomError(
                    it.message ?: "Unknown error",
                ),
            )
        }

        val signedTransaction = sign(
            amount = amount,
            sourceAddress = wallet.address,
            destinationAddress = destination,
            context = currentContext,
            signer = DummyPolkadotTransactionSigner(),
        ).successOr { return it }

        val fee = networkProvider.getFee(signedTransaction).successOr { return it }
        val feeAmount = amount.copy(value = fee)

        return Result.Success(TransactionFee.Single(Fee.Common(feeAmount)))
    }

    override fun createTransaction(amount: Amount, fee: Fee, destination: String): TransactionData.Uncompiled {
        return when (amount.type) {
            AmountType.Coin -> super.createTransaction(amount, fee, destination)
            else -> throw UnsupportedOperation()
        }
    }

    @Deprecated("Will be removed in the future. Use TransactionValidator instead")
    override fun validateTransaction(amount: Amount, fee: Amount?): EnumSet<TransactionError> {
        val errors = super.validateTransaction(amount, fee)

        val totalToSend = fee?.value?.add(amount.value) ?: amount.value ?: BigDecimal.ZERO
        val balance = wallet.amounts[AmountType.Coin]!!.value ?: BigDecimal.ZERO

        val remainBalance = balance.minus(totalToSend)
        if (remainBalance < existentialDeposit) {
            errors.add(TransactionError.AmountLowerExistentialDeposit)
        }
        return errors
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        transactionData.requireUncompiled()

        runCatching { updateEra() }
            .onFailure { return Result.Failure(BlockchainSdkError.CustomError(it.message ?: "Unknown error")) }
        return when (transactionData.amount.type) {
            AmountType.Coin -> sendCoin(transactionData, signer, currentContext)
            else -> Result.Failure(UnsupportedOperation())
        }
    }

    override suspend fun sendMultiple(
        transactionDataList: List<TransactionData>,
        signer: TransactionSigner,
        sendMode: TransactionSender.MultipleTransactionSendMode,
    ): Result<TransactionsSendResult> {
        if (transactionDataList.size == 1) {
            return sendSingleTransaction(transactionDataList, signer)
        }

        val compiledTransactionList = transactionDataList.map {
            it.requireCompiled()
        }

        val signResult = signMultipleCompiledTransactionData(
            compiledTransactionList = compiledTransactionList,
            signer = signer,
            publicKey = wallet.publicKey,
        )

        return when (signResult) {
            is Result.Failure -> Result.Failure(signResult.error)
            is Result.Success -> {
                coroutineScope {
                    val sendResults = signResult.data.mapIndexed { index, signedData ->
                        if (index != 0) {
                            delay(SEND_TRANSACTIONS_DELAY)
                        }

                        when (val sendResult = networkProvider.sendTransaction(signedData)) {
                            is Result.Failure -> sendResult
                            is Result.Success -> {
                                val hash = sendResult.data.formatHex()
                                transactionDataList[index].hash = hash
                                wallet.addOutgoingTransaction(transactionDataList[index].updateHash(hash = hash))
                                Result.Success(TransactionSendResult(hash))
                            }
                        }
                    }
                    val failedResult = sendResults.firstOrNull { it is Result.Failure }
                    if (failedResult != null) {
                        Result.Failure((failedResult as Result.Failure).error)
                    } else {
                        Result.Success(
                            TransactionsSendResult(sendResults.mapNotNull { (it as? Result.Success)?.data?.hash }),
                        )
                    }
                }
            }
        }
    }

    private suspend fun updateEra() {
        val latestBlockHash = networkProvider.getLatestBlockHash().successOr { error("latestBlockHash error") }
        val blockNumber = networkProvider.getBlockNumber(latestBlockHash).successOr { error("blockNumber error") }
        currentContext.era = makeEraFromBlockNumber(blockNumber.toLong())
        currentContext.eraBlockHash = Hash256(latestBlockHash.hexToBytes())
    }

    private suspend fun sendCoin(
        transactionData: TransactionData,
        signer: TransactionSigner,
        extrinsicContext: ExtrinsicContext,
    ): Result<TransactionSendResult> {
        transactionData.requireUncompiled()

        val destinationAddress = transactionData.destinationAddress
        val isDestinationAccountIsUnderfunded = isAccountUnderfunded(destinationAddress).successOr {
            return Result.Failure(it.error)
        }

        if (isDestinationAccountIsUnderfunded) {
            val amountValueToSend = transactionData.amount.value ?: BigDecimal.ZERO
            if (amountValueToSend < existentialDeposit) {
                val minReserve = Amount(transactionData.amount, existentialDeposit)
                return Result.Failure(BlockchainSdkError.CreateAccountUnderfunded(wallet.blockchain, minReserve))
            }
        }

        val signedTransaction = sign(
            amount = transactionData.amount,
            sourceAddress = wallet.address,
            destinationAddress = destinationAddress,
            context = extrinsicContext,
            signer = signer,
        ).successOr { return Result.Failure(it.error) }

        val txHash = networkProvider.sendTransaction(signedTransaction).successOr {
            return Result.Failure(it.error)
        }

        val hash = txHash.formatHex()
        transactionData.hash = hash
        transactionData.date = Calendar.getInstance()
        wallet.addOutgoingTransaction(transactionData)

        return Result.Success(TransactionSendResult(hash))
    }

    private suspend fun sign(
        amount: Amount,
        sourceAddress: String,
        destinationAddress: String,
        context: ExtrinsicContext,
        signer: TransactionSigner,
    ): Result<ByteArray> {
        val builtTxForSign = txBuilder.buildForSign(destinationAddress, amount, context)

        return when (val signResult = signer.sign(builtTxForSign, wallet.publicKey)) {
            is CompletionResult.Success -> {
                val signature = signResult.data
                val builtForSend = txBuilder.buildForSend(sourceAddress, destinationAddress, amount, context, signature)
                Result.Success(builtForSend)
            }
            is CompletionResult.Failure -> Result.fromTangemSdkError(signResult.error)
        }
    }

    private suspend fun signMultipleCompiledTransactionData(
        compiledTransactionList: List<TransactionData.Compiled>,
        signer: TransactionSigner,
        publicKey: Wallet.PublicKey,
    ): Result<List<ByteArray>> {
        val builtTxForSignList = compiledTransactionList.map {
            txBuilder.buildForSignCompiled(it)
        }

        return when (val signResults = signer.sign(builtTxForSignList, publicKey)) {
            is CompletionResult.Success -> {
                val builtForSend = signResults.data.mapIndexed { index, hash ->
                    txBuilder.buildForSendCompiled(
                        transaction = compiledTransactionList[index],
                        signedPayload = hash,
                    )
                }
                Result.Success(builtForSend)
            }

            is CompletionResult.Failure -> {
                Result.fromTangemSdkError(signResults.error)
            }
        }
    }

    private suspend fun isAccountUnderfunded(address: String): Result<Boolean> {
        val destinationBalance = networkProvider.getBalance(address).successOr { return it }
        val isUnderfunded =
            destinationBalance == BigDecimal.ZERO || destinationBalance < existentialDeposit
        return Result.Success(isUnderfunded)
    }

    private companion object {
        const val SEND_TRANSACTIONS_DELAY = 5_000L
    }
}