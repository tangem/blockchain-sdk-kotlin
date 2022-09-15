package com.tangem.blockchain.blockchains.polkadot

import com.tangem.blockchain.blockchains.polkadot.polkaj.extensions.*
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.BlockchainSdkError.UnsupportedOperation
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.successOr
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import io.emeraldpay.polkaj.ss58.SS58Type
import io.emeraldpay.polkaj.tx.ExtrinsicContext
import io.emeraldpay.polkaj.types.Address
import io.emeraldpay.polkaj.types.DotAmount
import java.math.BigDecimal
import java.util.*

/**
[REDACTED_AUTHOR]
 */
class PolkadotWalletManager(
    wallet: Wallet,
    val network: SS58Type.Network,
    private val networkService: PolkadotNetworkService
) : WalletManager(wallet), TransactionSender, ExistentialDepositProvider {

    private lateinit var currentContext: ExtrinsicContext

    private val accountAddress = Address(network, wallet.publicKey.blockchainKey)
    private val txBuilder = PolkadotTransactionBuilder(network)

    override val currentHost: String = network.url

    override fun getExistentialDeposit(): BigDecimal = network.existentialDeposit

    override suspend fun update() {
        val dotAmount: DotAmount = networkService.getBalance(accountAddress).successOr {
            wallet.removeAllTokens()
            throw (it.error as BlockchainSdkError)
        }

        wallet.setCoinValue(dotAmount.toBigDecimal(network))
        updateRecentTransactions()
    }

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

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        currentContext = networkService.extrinsicContext(accountAddress).successOr { return it }

        val signedTransaction = sign(
            amount = amount,
            sourceAddress = accountAddress,
            destinationAddress = Address.from(destination),
            context = currentContext,
            signer = DummyPolkadotTransactionSigner()
        ).successOr { return it }

        val feeDot = networkService.getFee(signedTransaction).successOr { return it }
        val feeAmount = amount.copy(value = feeDot.toBigDecimal(network))

        return Result.Success(listOf(feeAmount))
    }

    override fun createTransaction(amount: Amount, fee: Amount, destination: String): TransactionData {
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
        if (remainBalance < getExistentialDeposit()) {
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
        val destinationAddress = Address.from(transactionData.destinationAddress)
        val destinationAccountIsUnderfunded = accountIsUnderfunded(destinationAddress).successOr {
            return SimpleResult.Failure(it.error)
        }

        if (destinationAccountIsUnderfunded) {
            val amountValueToSend = transactionData.amount.value ?: BigDecimal.ZERO
            if (amountValueToSend < getExistentialDeposit()) {
                val minReserve = Amount(transactionData.amount, getExistentialDeposit())
                return SimpleResult.Failure(BlockchainSdkError.CreateAccountUnderfunded(wallet.blockchain, minReserve))
            }
        }

        val signedTransaction = sign(
            amount = transactionData.amount,
            sourceAddress = accountAddress,
            destinationAddress = destinationAddress,
            context = currentContext,
            signer = signer
        ).successOr { return SimpleResult.Failure(it.error) }

        val hash256 = networkService.sendTransaction(signedTransaction).successOr {
            return SimpleResult.Failure(it.error)
        }

        transactionData.hash = hash256.bytes.toHexString()
        transactionData.date = Calendar.getInstance()
        wallet.addOutgoingTransaction(transactionData)

        return SimpleResult.Success
    }

    private suspend fun sign(
        amount: Amount,
        sourceAddress: Address,
        destinationAddress: Address,
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

    private suspend fun accountIsUnderfunded(address: Address): Result<Boolean> {
        val destinationBalance = networkService.getBalance(address).successOr { return it }.toBigDecimal(network)
        val isUnderfunded = destinationBalance == BigDecimal.ZERO || destinationBalance < getExistentialDeposit()
        return Result.Success(isUnderfunded)
    }
}