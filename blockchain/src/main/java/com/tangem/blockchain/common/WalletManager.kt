package com.tangem.blockchain.common

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.isAboveZero
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.isZero
import java.math.BigDecimal
import java.util.*

abstract class WalletManager(
        var wallet: Wallet,
        val presetTokens: MutableSet<Token> = mutableSetOf(),
) {

    var dustValue: BigDecimal? = null

    abstract suspend fun update()

    protected open fun updateRecentTransactionsBasic(transactions: List<BasicTransactionData>) {
        val (confirmedTransactions, unconfirmedTransactions) =
                transactions.partition { it.isConfirmed }

        wallet.recentTransactions.forEach { recent ->
            if (confirmedTransactions.find { confirmed ->
                        confirmed.hash.equals(recent.hash, true)
                    } != null
            ) {
                recent.status = TransactionStatus.Confirmed
            }
        }
        unconfirmedTransactions.forEach { unconfirmed ->
            if (wallet.recentTransactions.find { recent ->
                        recent.hash.equals(unconfirmed.hash, true)
                    } == null
            ) {
                wallet.recentTransactions.add(unconfirmed.toTransactionData())
            }
        }
    }

    protected fun updateRecentTransactions(transactions: List<TransactionData>) {
        val (confirmedTransactions, unconfirmedTransactions) =
                transactions.partition { it.status == TransactionStatus.Confirmed }

        wallet.recentTransactions.forEach { recent ->
            if (confirmedTransactions.find { confirmed ->
                        confirmed.hash.equals(recent.hash, true)
                    } != null
            ) {
                recent.status = TransactionStatus.Confirmed
            }
        }
        unconfirmedTransactions.forEach { unconfirmed ->
            if (wallet.recentTransactions.find { recent ->
                        recent.hash.equals(unconfirmed.hash, true)
                    } == null
            ) {
                wallet.recentTransactions.add(unconfirmed)
            }
        }
    }

    fun createTransaction(amount: Amount, fee: Amount, destination: String): TransactionData {
        val contractAddress = if (amount.type is AmountType.Token) {
            amount.type.token.contractAddress
        } else {
            null
        }
        return TransactionData(amount, fee,
                wallet.address, destination, contractAddress,
                TransactionStatus.Unconfirmed, Calendar.getInstance(), null)
    }

    // TODO: add decimals and currency checks?
    open fun validateTransaction(amount: Amount, fee: Amount?): EnumSet<TransactionError> {
        val errors = EnumSet.noneOf(TransactionError::class.java)

        if (!validateAmountValue(amount)) errors.add(TransactionError.InvalidAmountValue)
        if (!validateAmountAvalible(amount)) errors.add(TransactionError.AmountExceedsBalance)
        if (fee == null) return errors

        if (!validateAmountValue(fee)) errors.add(TransactionError.InvalidFeeValue)
        if (!validateAmountAvalible(fee)) errors.add(TransactionError.FeeExceedsBalance)

        val total: BigDecimal
        if (amount.type == AmountType.Coin) {
            total = (amount.value ?: BigDecimal.ZERO) + (fee.value ?: BigDecimal.ZERO)
            if (!validateAmountAvalible(Amount(amount, total))) errors.add(TransactionError.TotalExceedsBalance)
        } else {
            total = amount.value ?: BigDecimal.ZERO
        }

        if (dustValue != null) {
            if (!validateNotDust(amount)) errors.add(TransactionError.DustAmount)
            val change = wallet.amounts[AmountType.Coin]!!.value!! - total
            if (!validateNotDust(Amount(amount, change))) errors.add(TransactionError.DustChange)
        }
        return errors
    }

    fun removeToken(token: Token) {
        presetTokens.remove(token)
        wallet.removeToken(token)
    }

    open suspend fun addToken(token: Token): Result<Amount> {
        return Result.Failure(Exception("Adding tokens not supported for ${wallet.blockchain.currency}"))
    }

    private fun validateAmountValue(amount: Amount) = amount.isAboveZero()

    private fun validateAmountAvalible(amount: Amount): Boolean {
        return wallet.fundsAvailable(amount.type) >= amount.value
    }

    private fun validateNotDust(amount: Amount): Boolean {
        if (dustValue == null) return true
        return dustValue!! <= amount.value || amount.value!!.isZero()
    }

    private fun BasicTransactionData.toTransactionData(): TransactionData {
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

interface TransactionPusher {
    suspend fun isPushAvailable(transactionHash: String): Result<Boolean>
    suspend fun getTransactionData(transactionHash: String): Result<TransactionData>
    suspend fun getPushFee(
            transactionHash: String,
            amount: Amount,
            destination: String
    ): Result<List<Amount>>

    suspend fun push(
            transactionHash: String,
            newTransactionData: TransactionData,
            signer: TransactionSigner
    ): SimpleResult
}

interface TransactionSigner {
    suspend fun sign(
            hashes: List<ByteArray>, cardId: String, walletPublicKey: ByteArray
    ): CompletionResult<List<ByteArray>>

    suspend fun sign(
            hash: ByteArray, cardId: String, walletPublicKey: ByteArray
    ): CompletionResult<ByteArray>
}

interface SignatureCountValidator {
    suspend fun validateSignatureCount(signedHashes: Int): SimpleResult
}

interface TokenFinder {
    suspend fun findTokens() : Result<List<Token>>
}