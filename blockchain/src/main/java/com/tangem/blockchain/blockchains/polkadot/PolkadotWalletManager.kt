package com.tangem.blockchain.blockchains.polkadot

import com.tangem.blockchain.blockchains.polkadot.network.PolkadotNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.BlockchainSdkError.UnsupportedOperation
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.successOr
import com.tangem.common.CompletionResult
import io.emeraldpay.polkaj.tx.ExtrinsicContext
import java.math.BigDecimal
import java.util.Calendar
import java.util.EnumSet

/**
 * Created by Anton Zhilenkov on 10/06/2022.
 */
class PolkadotWalletManager(
    wallet: Wallet,
    private val networkProvider: PolkadotNetworkProvider,
) : WalletManager(wallet), TransactionSender, ExistentialDepositProvider {

    private lateinit var currentContext: ExtrinsicContext

    private val existentialDeposit: BigDecimal = when (wallet.blockchain) {
        Blockchain.Polkadot -> BigDecimal.ONE
        Blockchain.PolkadotTestnet -> 0.01.toBigDecimal()
        Blockchain.Kusama -> 0.000033333333.toBigDecimal()
        Blockchain.AlephZero, Blockchain.AlephZeroTestnet -> 0.0000000005.toBigDecimal()
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
            .filter { it.hash != null && it.date != null }
            .filter {
                val txTimeInMillis = it.date?.timeInMillis ?: currentTimeInMillis
                currentTimeInMillis - txTimeInMillis > 9999
            }.map {
                it.copy(status = TransactionStatus.Confirmed)
            }

        updateRecentTransactions(confirmedTxData)
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        currentContext = networkProvider.extrinsicContext(wallet.address).successOr { return it }

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

    override fun createTransaction(amount: Amount, fee: Fee, destination: String): TransactionData {
        return when (amount.type) {
            AmountType.Coin -> super.createTransaction(amount, fee, destination)
            else -> throw UnsupportedOperation()
        }
    }

    override fun validateTransaction(amount: Amount, fee: Amount?): EnumSet<TransactionError> {
        val errors = super.validateTransaction(amount, fee)

        val totalToSend = fee?.value?.add(amount.value) ?: amount.value ?: BigDecimal.ZERO
        val balance = wallet.amounts[AmountType.Coin]!!.value ?: BigDecimal.ZERO
        if (totalToSend == balance) return errors

        val remainBalance = balance.minus(totalToSend)
        if (remainBalance < existentialDeposit) {
            errors.add(TransactionError.AmountLowerExistentialDeposit)
        }
        return errors
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        return when (transactionData.amount.type) {
            AmountType.Coin -> sendCoin(transactionData, signer)
            else -> SimpleResult.Failure(UnsupportedOperation())
        }
    }

    private suspend fun sendCoin(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val destinationAddress = transactionData.destinationAddress
        val isDestinationAccountIsUnderfunded = isAccountUnderfunded(destinationAddress).successOr {
            return SimpleResult.Failure(it.error)
        }

        if (isDestinationAccountIsUnderfunded) {
            val amountValueToSend = transactionData.amount.value ?: BigDecimal.ZERO
            if (amountValueToSend < existentialDeposit) {
                val minReserve = Amount(transactionData.amount, existentialDeposit)
                return SimpleResult.Failure(BlockchainSdkError.CreateAccountUnderfunded(wallet.blockchain, minReserve))
            }
        }

        val signedTransaction = sign(
            amount = transactionData.amount,
            sourceAddress = wallet.address,
            destinationAddress = destinationAddress,
            context = currentContext,
            signer = signer,
        ).successOr { return SimpleResult.Failure(it.error) }

        val txHash = networkProvider.sendTransaction(signedTransaction).successOr {
            return SimpleResult.Failure(it.error)
        }

        transactionData.hash = txHash
        transactionData.date = Calendar.getInstance()
        wallet.addOutgoingTransaction(transactionData)

        return SimpleResult.Success
    }

    private suspend fun sign(
        amount: Amount,
        sourceAddress: String,
        destinationAddress: String,
        context: ExtrinsicContext,
        signer: TransactionSigner,
    ): Result<ByteArray> {
        val builtTxForSign = txBuilder.buildForSign(destinationAddress, amount, context)

        val signResult = signer.sign(builtTxForSign, wallet.publicKey)
        return when (signResult) {
            is CompletionResult.Success -> {
                val signature = signResult.data
                val builtForSend = txBuilder.buildForSend(sourceAddress, destinationAddress, amount, context, signature)
                Result.Success(builtForSend)
            }
            is CompletionResult.Failure -> Result.fromTangemSdkError(signResult.error)
        }
    }

    private suspend fun isAccountUnderfunded(address: String): Result<Boolean> {
        val destinationBalance = networkProvider.getBalance(address).successOr { return it }
        val isUnderfunded =
            destinationBalance == BigDecimal.ZERO || destinationBalance < existentialDeposit
        return Result.Success(isUnderfunded)
    }
}
