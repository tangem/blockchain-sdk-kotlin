package com.tangem.blockchain.common

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.isAboveZero
import com.tangem.commands.SignResponse
import com.tangem.common.CompletionResult
import java.math.BigDecimal
import java.util.*

abstract class WalletManager(val cardId: String, var wallet: Wallet) {

    abstract suspend fun update()

    protected open fun updateRecentTransactions(transactions: List<BasicTransactionData>) {
        val (confirmedTransactions, unconfirmedTransactions) =
                transactions.partition { it.isConfirmed }

        wallet.recentTransactions.forEach {
            if (confirmedTransactions.find {confirmed -> confirmed.hash == it.hash } != null) {
                it.status = TransactionStatus.Confirmed
            }
        }
        unconfirmedTransactions.forEach {
            if (wallet.recentTransactions.find { unconfirmed -> unconfirmed.hash == it.hash } == null) {
                wallet.recentTransactions.add(it.toTransactionData())
            }
        }
        wallet.sentTransactionsCount = transactions.filter { it.balanceDif < 0.toBigDecimal() }.size
    }

    fun createTransaction(amount: Amount, fee: Amount, destination: String): TransactionData {
        return TransactionData(amount, fee,
                    wallet.address, destination, wallet.token?.contractAddress,
                    TransactionStatus.Unconfirmed, Calendar.getInstance(), null)
    }

    fun validateTransaction(amount: Amount, fee: Amount?): EnumSet<TransactionError> {
        val errors = EnumSet.noneOf(TransactionError::class.java)

        if (!validateAmount(amount)) errors.add(TransactionError.WrongAmount)
        if (fee == null) return errors

        if (!validateAmount(fee)) errors.add(TransactionError.WrongFee)
        val total = (amount.value ?: BigDecimal.ZERO) + (fee.value ?: BigDecimal.ZERO)
        if (!validateAmount(Amount(amount, total))) errors.add(TransactionError.WrongTotal)

        return errors
    }

    private fun validateAmount(amount: Amount): Boolean {
        return amount.isAboveZero() &&
                wallet.fundsAvailable(amount.type) >= amount.value
    }

    protected fun BasicTransactionData.toTransactionData(): TransactionData {
        val isIncoming = this.balanceDif.signum() > 0
        return TransactionData(
                amount = Amount(wallet.amounts[AmountType.Coin]!!, this.balanceDif.abs()),
                fee = null,
                sourceAddress = if (isIncoming) "unknown" else wallet.address,
                destinationAddress = if (isIncoming) wallet.address else "unknown",
                hash = this.hash,
                date = this.date,
                status = if (this.isConfirmed) {
                    TransactionStatus.Confirmed
                } else {
                    TransactionStatus.Unconfirmed
                }
        )
    }
}

interface TransactionSender {
    suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult
    suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>>

}

interface TransactionSigner {
    suspend fun sign(hashes: Array<ByteArray>, cardId: String): CompletionResult<SignResponse>
}

interface SignatureCountValidator {
    suspend fun validateSignatureCount(signedHashes: Int): SimpleResult
}