package com.tangem.blockchain.common

import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.common.transaction.TransactionsSendResult
import com.tangem.blockchain.extensions.*
import com.tangem.blockchain.nft.DefaultNFTProvider
import com.tangem.blockchain.nft.NFTProvider
import com.tangem.blockchain.transactionhistory.DefaultTransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.isZero
import com.tangem.operations.sign.SignData
import java.math.BigDecimal
import java.util.Calendar
import java.util.EnumSet

abstract class WalletManager(
    var wallet: Wallet,
    val cardTokens: MutableSet<Token> = mutableSetOf(),
    transactionHistoryProvider: TransactionHistoryProvider = DefaultTransactionHistoryProvider,
    nftProvider: NFTProvider = DefaultNFTProvider,
) : TransactionSender,
    TransactionHistoryProvider by transactionHistoryProvider,
    NFTProvider by nftProvider {

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

    override suspend fun estimateFee(
        amount: Amount,
        destination: String,
        callData: SmartContractCallData?,
    ): Result<TransactionFee> {
        return getFee(amount, destination, callData)
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

    protected fun updateRecentTransactions(transactions: List<TransactionData.Uncompiled>) {
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

    open fun createTransaction(amount: Amount, fee: Fee, destination: String): TransactionData.Uncompiled {
        return TransactionData.Uncompiled(
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
    @Deprecated("Will be removed in the future. Use TransactionValidator instead")
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

    private fun BasicTransactionData.toTransactionData(): TransactionData.Uncompiled {
        return TransactionData.Uncompiled(
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

    override suspend fun sendMultiple(
        transactionDataList: List<TransactionData>,
        signer: TransactionSigner,
        sendMode: TransactionSender.MultipleTransactionSendMode,
    ): Result<TransactionsSendResult> {
        return sendSingleTransaction(transactionDataList, signer)
    }

    protected suspend fun sendSingleTransaction(
        transactionDataList: List<TransactionData>,
        signer: TransactionSigner,
    ): Result<TransactionsSendResult> {
        return send(transactionDataList[0], signer).fold(
            success = { Result.Success(TransactionsSendResult(hashes = listOf(it.hash))) },
            failure = { Result.Failure(it) },
        )
    }

    companion object
}

interface TransactionSender {

    enum class MultipleTransactionSendMode {
        DEFAULT,
        WAIT_AFTER_FIRST,
    }

    suspend fun sendMultiple(
        transactionDataList: List<TransactionData>,
        signer: TransactionSigner,
        sendMode: MultipleTransactionSendMode = MultipleTransactionSendMode.DEFAULT,
    ): Result<TransactionsSendResult>

    suspend fun send(transactionData: TransactionData, signer: TransactionSigner): Result<TransactionSendResult>

    // Think about migration to different interface
    suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee>

    suspend fun getFee(
        amount: Amount,
        destination: String,
        callData: SmartContractCallData? = null,
    ): Result<TransactionFee> = getFee(
        amount = amount,
        destination = destination,
    )

    suspend fun getFee(transactionData: TransactionData): Result<TransactionFee> {
        transactionData.requireUncompiled()
        return getFee(
            amount = transactionData.amount,
            destination = transactionData.destinationAddress,
        )
    }

    /**
     * Estimates fee (approximate value)
     *
     * [Think about migration to interface]
     */
    suspend fun estimateFee(
        amount: Amount,
        destination: String,
        callData: SmartContractCallData? = null,
    ): Result<TransactionFee> = getFee(
        amount = amount,
        destination = destination,
        callData = callData,
    )
}

interface TransactionSigner {
    suspend fun sign(hashes: List<ByteArray>, publicKey: Wallet.PublicKey): CompletionResult<List<ByteArray>>

    suspend fun sign(hash: ByteArray, publicKey: Wallet.PublicKey): CompletionResult<ByteArray>

    suspend fun multiSign(
        dataToSign: List<SignData>,
        publicKey: Wallet.PublicKey,
    ): CompletionResult<Map<ByteArray, ByteArray>>
}

interface TransactionValidator {

    suspend fun validate(transactionData: TransactionData): kotlin.Result<Unit>
}

interface TransactionPreparer {
    suspend fun prepareForSend(transactionData: TransactionData, signer: TransactionSigner): Result<ByteArray>

    suspend fun prepareAndSign(transactionData: TransactionData, signer: TransactionSigner): Result<ByteArray>

    suspend fun prepareForSendMultiple(
        transactionDataList: List<TransactionData>,
        signer: TransactionSigner,
    ): Result<List<ByteArray>> {
        return prepareForSend(transactionDataList.first(), signer).fold(
            success = { Result.Success(listOf(it)) },
            failure = { Result.Failure(it) },
        )
    }

    suspend fun prepareAndSignMultiple(
        transactionDataList: List<TransactionData>,
        signer: TransactionSigner,
    ): Result<List<ByteArray>> {
        return prepareAndSign(transactionDataList.first(), signer).fold(
            success = { Result.Success(listOf(it)) },
            failure = { Result.Failure(it) },
        )
    }
}

interface SignatureCountValidator {
    suspend fun validateSignatureCount(signedHashes: Int): SimpleResult
}

interface TokenFinder {
    suspend fun findTokens(): Result<List<Token>>
}

interface Approver {
    suspend fun getAllowance(spenderAddress: String, token: Token): kotlin.Result<BigDecimal>

    fun getApproveData(spenderAddress: String, value: Amount? = null): String
}

/**
 * Interface for resolving human readable names to addresses
 */
interface NameResolver {
    /**
     * Resolves a human readable name to an address.
     *
     * @param name The human readable name to resolve.
     * @return A [ResolveAddressResult] containing the resolved address or an error.
     */
    suspend fun resolve(name: String): ResolveAddressResult
}

/**
 * Common interface for UTXO blockchain managers
 */
interface UtxoBlockchainManager {
    /** Indicates allowance of self sending */
    val allowConsolidation: Boolean
}