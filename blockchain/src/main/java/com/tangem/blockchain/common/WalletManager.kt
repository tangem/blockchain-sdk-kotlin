package com.tangem.blockchain.common

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.txhistory.DefaultTransactionHistoryProvider
import com.tangem.blockchain.common.txhistory.TransactionHistoryProvider
import com.tangem.blockchain.extensions.DebouncedInvoke
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.isAboveZero
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.isZero
import java.math.BigDecimal
import java.util.Calendar
import java.util.EnumSet

abstract class WalletManager(
    var wallet: Wallet,
    val cardTokens: MutableSet<Token> = mutableSetOf(),
    transactionHistoryProvider: TransactionHistoryProvider = DefaultTransactionHistoryProvider,
) : TransactionHistoryProvider by transactionHistoryProvider, TransactionSender {

    open val allowsFeeSelection: FeeSelectionState = FeeSelectionState.Unspecified

    var outputsCount: Int? = null
        internal set

    abstract val currentHost: String

    open val dustValue: BigDecimal? = null

    private val updateDebounced = DebouncedInvoke()

    /**
     * Update wallet state. [forceUpdate] to skip debounce
     */
    suspend fun update(forceUpdate: Boolean = false) {
        updateDebounced.invokeOnExpire(forceUpdate) {
            updateInternal()
        }
    }

    override suspend fun estimateFee(amount: Amount, destination: String): Result<TransactionFee> {
        return getFee(amount, destination)
    }

    internal abstract suspend fun updateInternal()

    protected open fun updateRecentTransactionsBasic(transactions: List<BasicTransactionData>) {
        val (confirmedTransactions, unconfirmedTransactions) = transactions.partition { it.isConfirmed }

        wallet.recentTransactions.forEach { recent ->
            val confirmedTx = confirmedTransactions.find { confirmed -> confirmed.hash.equals(recent.hash, true) }
            if (confirmedTx != null) {
                recent.status = TransactionStatus.Confirmed
            }
        }

        unconfirmedTransactions.forEach { unconfirmed ->
            val recentTx = wallet.recentTransactions.find { recent -> recent.hash.equals(unconfirmed.hash, true) }
            if (recentTx == null) {
                wallet.recentTransactions.add(unconfirmed.toTransactionData())
            }
        }
    }

    protected fun updateRecentTransactions(transactions: List<TransactionData>) {
        val (confirmedTransactions, unconfirmedTransactions) =
            transactions.partition { it.status == TransactionStatus.Confirmed }

        wallet.recentTransactions.forEach { recent ->
            val confirmedTx = confirmedTransactions.find { confirmed -> confirmed.hash.equals(recent.hash, true) }
            if (confirmedTx != null) {
                recent.status = TransactionStatus.Confirmed
            }
        }
        unconfirmedTransactions.forEach { unconfirmed ->
            val recentTx = wallet.recentTransactions.find { recent -> recent.hash.equals(unconfirmed.hash, true) }
            if (recentTx == null) {
                wallet.recentTransactions.add(unconfirmed)
            }
        }
    }

    open fun createTransaction(amount: Amount, fee: Fee, destination: String): TransactionData {
        return TransactionData(
            amount = amount,
            fee = fee,
            sourceAddress = wallet.address,
            destinationAddress = destination,
            status = TransactionStatus.Unconfirmed,
            date = Calendar.getInstance(),
            hash = null,
        )
    }

    // TODO: add decimals and currency checks?
    open fun validateTransaction(amount: Amount, fee: Amount?): EnumSet<TransactionError> {
        val errors = EnumSet.noneOf(TransactionError::class.java)

        if (!validateAmountValue(amount)) errors.add(TransactionError.InvalidAmountValue)
        if (!validateAmountAvalible(amount)) errors.add(TransactionError.AmountExceedsBalance)
        if (fee == null) return errors

        if (!validateAmountAvalible(fee)) errors.add(TransactionError.FeeExceedsBalance)

        val total: BigDecimal
        if (amount.type == AmountType.Coin && amount.currencySymbol == fee.currencySymbol) {
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

    open fun removeToken(token: Token) {
        cardTokens.remove(token)
        wallet.removeToken(token)
    }

    open fun addToken(token: Token) {
        if (!cardTokens.contains(token)) {
            cardTokens.add(token)
        }
    }

    open fun addTokens(tokens: List<Token>) {
        tokens.forEach { addToken(it) }
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
        return TransactionData(
            amount = Amount(wallet.amounts[AmountType.Coin]!!, this.balanceDif.abs()),
            fee = null,
            sourceAddress = source,
            destinationAddress = destination,
            hash = this.hash,
            date = this.date,
            status = if (this.isConfirmed) {
                TransactionStatus.Confirmed
            } else {
                TransactionStatus.Unconfirmed
            },
        )
    }

    companion object
}

interface TransactionSender {

    suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult

    // Think about migration to different interface
    suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee>

    /**
     * Estimates fee (approximate value)
     *
     * [Think about migration to interface]
     */
    suspend fun estimateFee(amount: Amount, destination: String): Result<TransactionFee>
}

interface TransactionSigner {
    suspend fun sign(hashes: List<ByteArray>, publicKey: Wallet.PublicKey): CompletionResult<List<ByteArray>>

    suspend fun sign(hash: ByteArray, publicKey: Wallet.PublicKey): CompletionResult<ByteArray>
}

interface SignatureCountValidator {
    suspend fun validateSignatureCount(signedHashes: Int): SimpleResult
}

interface TokenFinder {
    suspend fun findTokens(): Result<List<Token>>
}

interface Approver {
    suspend fun getAllowance(spenderAddress: String, token: Token): Result<BigDecimal>

    fun getApproveData(spenderAddress: String, value: Amount? = null): String
}
