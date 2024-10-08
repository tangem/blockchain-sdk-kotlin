package com.tangem.blockchain.blockchains.solana

import android.os.SystemClock
import android.util.Log
import com.tangem.blockchain.blockchains.solana.solanaj.core.SolanaTransaction
import com.tangem.blockchain.blockchains.solana.solanaj.model.SolanaMainAccountInfo
import com.tangem.blockchain.blockchains.solana.solanaj.model.SolanaSplAccountInfo
import com.tangem.blockchain.blockchains.solana.solanaj.model.TransactionInfo
import com.tangem.blockchain.blockchains.solana.solanaj.rpc.SolanaRpcClient
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.BlockchainSdkError.UnsupportedOperation
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.common.transaction.TransactionsSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.filterWith
import com.tangem.blockchain.extensions.map
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.MultiNetworkProvider
import kotlinx.coroutines.*
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.programs.Program
import org.p2p.solanaj.rpc.Cluster
import org.p2p.solanaj.rpc.types.config.Commitment
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
// FIXME: Refactor with wallet-core: [REDACTED_JIRA]
@Suppress("LargeClass")
class SolanaWalletManager internal constructor(
    wallet: Wallet,
    providers: List<SolanaRpcClient>,
) : WalletManager(wallet), TransactionSender, RentProvider {

    private val account = PublicKey(wallet.address)
    private val networkServices = providers.map { SolanaNetworkService(it) }

    private val multiNetworkProvider: MultiNetworkProvider<SolanaNetworkService> =
        MultiNetworkProvider(networkServices)
    private val tokenAccountInfoFinder = SolanaTokenAccountInfoFinder(multiNetworkProvider)
    private val transactionBuilder = SolanaTransactionBuilder(account, multiNetworkProvider, tokenAccountInfoFinder)

    private var accountSize: Long = MIN_ACCOUNT_DATA_SIZE

    override val currentHost: String
        get() = multiNetworkProvider.currentProvider.baseUrl

    private val feeRentHolder = mutableMapOf<Fee, BigDecimal>()
    override suspend fun updateInternal() {
        val accountInfo = multiNetworkProvider.performRequest {
            getMainAccountInfo(account)
        }.successOr {
            return updateWithError(it.error)
        }

        updateInternal(accountInfo)
    }

    private suspend fun updateInternal(accountInfo: SolanaMainAccountInfo) {
        accountSize = accountInfo.value?.space ?: MIN_ACCOUNT_DATA_SIZE
        wallet.setCoinValue(SolanaValueConverter.toSol(accountInfo.balance))

        updateRecentTransactions()
        addToRecentTransactions(accountInfo.txsInProgress)

        cardTokens.forEach { cardToken ->
            val tokenBalance =
                accountInfo.tokensByMint[cardToken.contractAddress]?.solAmount ?: BigDecimal.ZERO
            wallet.addTokenValue(tokenBalance, cardToken)
        }
    }

    private fun updateWithError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)

        wallet.removeAllTokens()
        throw error
    }

    private suspend fun updateRecentTransactions() {
        val txSignatures = wallet.recentTransactions.mapNotNull { it.hash }
        val signatureStatuses = multiNetworkProvider.performRequest {
            getSignatureStatuses(txSignatures)
        }.successOr {
            Log.e(this.javaClass.simpleName, it.error.customMessage)
            return
        }

        val confirmedTxData = mutableListOf<TransactionData.Uncompiled>()
        val signaturesStatuses = txSignatures.zip(signatureStatuses.value)
        signaturesStatuses.forEach { pair ->
            if (pair.second?.confirmationStatus == Commitment.FINALIZED.value) {
                val foundRecentTxData =
                    wallet.recentTransactions.firstOrNull { it.hash == pair.first }
                foundRecentTxData?.let {
                    confirmedTxData.add(
                        it.updateStatus(status = TransactionStatus.Confirmed) as TransactionData.Uncompiled,
                    )
                }
            }
        }
        updateRecentTransactions(confirmedTxData)
    }

    private fun addToRecentTransactions(txsInProgress: List<TransactionInfo>) {
        if (txsInProgress.isEmpty()) return

        val newTxsInProgress =
            txsInProgress.filterWith(wallet.recentTransactions) { a, b -> a.signature != b.hash }
        val newUnconfirmedTxData = newTxsInProgress.mapNotNull {
            if (it.instructions.isNotEmpty() && it.instructions[0].programId == Program.Id.system.toBase58()) {
                val info = it.instructions[0].parsed.info
                val amount = Amount(SolanaValueConverter.toSol(info.lamports), wallet.blockchain)
                val feeAmount = Amount(SolanaValueConverter.toSol(it.fee), wallet.blockchain)
                TransactionData.Uncompiled(
                    amount,
                    Fee.Common(feeAmount),
                    info.source,
                    info.destination,
                    TransactionStatus.Unconfirmed,
                    hash = it.signature,
                )
            } else {
                null
            }
        }
        wallet.recentTransactions.addAll(newUnconfirmedTxData)
    }

    override fun createTransaction(amount: Amount, fee: Fee, destination: String): TransactionData.Uncompiled {
        val accountCreationRent = feeRentHolder[fee]

        return if (accountCreationRent == null) {
            super.createTransaction(amount, fee, destination)
        } else {
            when (amount.type) {
                AmountType.Coin -> {
                    val newFee = Fee.Common(fee.amount.minus(accountCreationRent))
                    val newAmount = amount.plus(accountCreationRent)
                    super.createTransaction(newAmount, newFee, destination)
                }
                is AmountType.Token -> {
                    super.createTransaction(amount, fee, destination)
                }
                else -> throw UnsupportedOperation()
            }
        }
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        return when (transactionData) {
            is TransactionData.Compiled -> {
                val startSendingTimestamp = SystemClock.elapsedRealtime()

                val compiledTransaction = if (transactionData.value is TransactionData.Compiled.Data.Bytes) {
                    transactionData.value.data
                } else {
                    return Result.Failure(BlockchainSdkError.CustomError("Compiled transaction must be in bytes"))
                }
                val transactionWithoutSignaturePlaceholder =
                    compiledTransaction.drop(SIGNATURE_PLACEHOLDER_LENGTH).toByteArray()

                val transaction = transactionBuilder.buildUnsignedTransaction(
                    builtTransaction = transactionWithoutSignaturePlaceholder,
                )

                val signResult = signer.sign(transactionWithoutSignaturePlaceholder, wallet.publicKey).successOr {
                    return Result.fromTangemSdkError(it.error)
                }

                val patchedTransactionData = TransactionData.Compiled(
                    value = TransactionData.Compiled.Data.Bytes(
                        data = byteArrayOf(1) + signResult + transactionWithoutSignaturePlaceholder,
                    ),
                )

                sendTransaction(transaction, patchedTransactionData, startSendingTimestamp)
            }
            is TransactionData.Uncompiled -> {
                val transaction = transactionBuilder.buildUnsignedTransaction(
                    destinationAddress = transactionData.destinationAddress,
                    amount = transactionData.amount,
                ).successOr { return it }

                val startSendingTimestamp = SystemClock.elapsedRealtime()

                val signResult = signer.sign(transaction.getSerializedMessage(), wallet.publicKey).successOr {
                    return Result.fromTangemSdkError(it.error)
                }
                transaction.addSignedDataSignature(signResult)

                sendTransaction(transaction, transactionData, startSendingTimestamp)
            }
        }
    }

    override suspend fun sendMultiple(
        transactionDataList: List<TransactionData>,
        signer: TransactionSigner,
    ): Result<TransactionsSendResult> {
        if (transactionDataList.size == 1) {
            return super<TransactionSender>.sendMultiple(transactionDataList, signer)
        }

        val startSendingTimestamp = SystemClock.elapsedRealtime()

        val withoutSignatureTransactions = transactionDataList.map {
            it.requireCompiled()

            val compiled = (it.value as? TransactionData.Compiled.Data.Bytes)?.data
                ?: return Result.Failure(UnsupportedOperation("Compiled transaction must be in bytes"))

            compiled.drop(SIGNATURE_PLACEHOLDER_LENGTH).toByteArray()
        }

        val unsignedTransactions = withoutSignatureTransactions.map(transactionBuilder::buildUnsignedTransaction)

        val sendResults = signMultipleCompiledTransactions(withoutSignatureTransactions, signer)
            .successOr { return it }
            .mapIndexed { index, transaction ->
                when (val result = sendTransaction(unsignedTransactions[index], transaction, startSendingTimestamp)) {
                    is Result.Failure -> result
                    is Result.Success -> {
                        val hash = result.data.hash
                        transactionDataList[index].hash = hash
                        wallet.addOutgoingTransaction(transactionDataList[index].updateHash(hash = hash))
                        Result.Success(TransactionSendResult(hash))
                    }
                }
            }

        val failedResult = sendResults.firstOrNull { it is Result.Failure }
        return if (failedResult != null) {
            Result.Failure((failedResult as Result.Failure).error)
        } else {
            Result.Success(TransactionsSendResult(sendResults.mapNotNull { (it as? Result.Success)?.data?.hash }))
        }
    }

    private suspend fun signMultipleCompiledTransactions(
        transactionToSign: List<ByteArray>,
        signer: TransactionSigner,
    ): Result<List<TransactionData.Compiled>> {
        val signResults = signer.sign(transactionToSign, wallet.publicKey).successOr {
            return Result.fromTangemSdkError(it.error)
        }

        return Result.Success(
            signResults.mapIndexed { index, signResult ->
                TransactionData.Compiled(
                    value = TransactionData.Compiled.Data.Bytes(
                        data = byteArrayOf(1) + signResult + transactionToSign[index],
                    ),
                )
            },
        )
    }

    private suspend fun sendTransaction(
        signedTransaction: SolanaTransaction,
        transactionData: TransactionData,
        startSendingTimestamp: Long,
    ): Result<TransactionSendResult> {
        val sendResults = coroutineScope {
            multiNetworkProvider.providers
                .map { provider ->
                    async {
                        val serializedTransaction = when (transactionData) {
                            is TransactionData.Compiled -> {
                                if (transactionData.value is TransactionData.Compiled.Data.Bytes) {
                                    transactionData.value.data
                                } else {
                                    return@async Result.Failure(
                                        BlockchainSdkError.CustomError("Compiled transaction must be in bytes"),
                                    )
                                }
                            }
                            is TransactionData.Uncompiled -> signedTransaction.serialize()
                        }
                        provider.sendTransaction(serializedTransaction, startSendingTimestamp)
                    }
                }
                .awaitAll()
        }
        val firstSuccessResult = sendResults
            .filterIsInstance<Result.Success<String>>()
            .firstOrNull()

        if (firstSuccessResult != null) {
            feeRentHolder.clear()
            val hash = firstSuccessResult.data
            transactionData.hash = hash
            wallet.addOutgoingTransaction(transactionData, hashToLowercase = false)

            return Result.Success(TransactionSendResult(hash))
        }

        val error = sendResults
            .filterIsInstance<Result.Failure>()
            .firstOrNull()
            ?.error
            ?: BlockchainSdkError.FailedToSendException
        return Result.Failure(error)
    }

    /**
     * This is not a natural fee, as it may contain additional information about the amount that may be required
     * to open an account. Later, when creating a transaction, this amount will be deducted from fee and added
     * to the amount of the main transfer
     */
    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        feeRentHolder.clear()
        val (networkFee, accountCreationRent) = getNetworkFeeAndAccountCreationRent(amount, destination)
            .successOr { return it }

        var feeAmount = Fee.Common(Amount(networkFee, wallet.blockchain))
        if (accountCreationRent > BigDecimal.ZERO) {
            feeAmount = feeAmount.copy(amount = feeAmount.amount + accountCreationRent)
            feeRentHolder[feeAmount] = accountCreationRent
        }

        return Result.Success(TransactionFee.Single(feeAmount))
    }

    private suspend fun getNetworkFeeAndAccountCreationRent(
        amount: Amount,
        destination: String,
    ): Result<Pair<BigDecimal, BigDecimal>> {
        val results = withContext(Dispatchers.IO) {
            awaitAll(
                async { getNetworkFee(amount, destination) },
                async { getAccountCreationRent(amount, destination) },
            )
        }
        val networkFee = results[0].successOr { return it }
        val accountCreationRent = results[1].successOr { return it }

        return Result.Success(data = networkFee to accountCreationRent)
    }

    private suspend fun getNetworkFee(amount: Amount, destination: String): Result<BigDecimal> {
        val transaction = transactionBuilder.buildUnsignedTransaction(
            destinationAddress = destination,
            amount = amount,
        ).successOr { return it }
        val result = multiNetworkProvider.performRequest {
            getFeeForMessage(transaction)
        }.successOr { return it }

        return Result.Success(result.value.let(SolanaValueConverter::toSol))
    }

    private suspend fun getAccountCreationRent(amount: Amount, destination: String): Result<BigDecimal> {
        val destinationAccount = PublicKey(destination)

        return when (amount.type) {
            is AmountType.Coin -> {
                getCoinAccountCreationRent(amount, destinationAccount)
            }
            is AmountType.Token -> {
                val mint = PublicKey(amount.type.token.contractAddress)
                getTokenAccountCreationRent(mint, destinationAccount)
            }
            else -> Result.Failure(UnsupportedOperation())
        }
    }

    private suspend fun getCoinAccountCreationRent(amount: Amount, destinationAccount: PublicKey): Result<BigDecimal> {
        val amountValue = amount.value
            ?: return Result.Failure(BlockchainSdkError.NPError("amountValue"))

        multiNetworkProvider.performRequest {
            getAccountInfoIfExist(destinationAccount)
        }.successOr { failure ->
            return if (failure.error is BlockchainSdkError.AccountNotFound) {
                getCoinAccountCreationRent(amountValue)
            } else {
                failure
            }
        }

        return Result.Success(BigDecimal.ZERO)
    }

    private suspend fun getCoinAccountCreationRent(balance: BigDecimal): Result<BigDecimal> {
        val balanceForRentExemption = getMinimalBalanceForRentExemptionInSol(MIN_ACCOUNT_DATA_SIZE)
            .successOr { return it }

        val rent = if (balance >= balanceForRentExemption) {
            BigDecimal.ZERO
        } else {
            balanceForRentExemption
        }

        return Result.Success(rent)
    }

    private suspend fun getTokenAccountCreationRent(
        mint: PublicKey,
        destinationAccount: PublicKey,
    ): Result<BigDecimal> {
        val (sourceTokenAccountInfo, programId) = tokenAccountInfoFinder.getTokenAccountInfoAndTokenProgramId(
            account = account,
            mint = mint,
        ).successOr { return it }

        tokenAccountInfoFinder.getTokenAccountInfoIfExist(
            account = destinationAccount,
            mint = mint,
            programId = programId,
        ).successOr { failure ->
            return if (failure.error is BlockchainSdkError.AccountNotFound) {
                getTokenAccountCreationRent(sourceTokenAccountInfo)
            } else {
                failure
            }
        }

        return Result.Success(BigDecimal.ZERO)
    }

    private suspend fun getTokenAccountCreationRent(sourceTokenAccountInfo: SolanaSplAccountInfo): Result<BigDecimal> {
        val sourceTokenAccountSize = requireNotNull(sourceTokenAccountInfo.value.data?.space) {
            "Source token account data must not be null"
        }
        val rent = getMinimalBalanceForRentExemptionInSol(sourceTokenAccountSize.toLong())
            .successOr { return it }

        return Result.Success(rent)
    }

    override suspend fun minimalBalanceForRentExemption(): Result<BigDecimal> {
        return getMinimalBalanceForRentExemptionInSol(accountSize)
    }

    // FIXME: The rent calculation is based on hardcoded values that may be changed in the future
    override suspend fun rentAmount(): BigDecimal {
        val accountSizeWithMetadata = (accountSize + ACCOUNT_METADATA_SIZE).toBigDecimal()
        val rentPerEpoch = determineRentPerEpoch(multiNetworkProvider.currentProvider).toBigDecimal()

        return accountSizeWithMetadata
            .multiply(rentPerEpoch)
            .let(SolanaValueConverter::toSol)
    }

    private suspend fun getMinimalBalanceForRentExemptionInSol(accountSize: Long): Result<BigDecimal> {
        return multiNetworkProvider.performRequest {
            minimalBalanceForRentExemption(accountSize)
        }
            .map(SolanaValueConverter::toSol)
    }

    private fun determineRentPerEpoch(provider: SolanaNetworkService): Double = when (provider.endpoint) {
        Cluster.TESTNET.endpoint -> RENT_PER_EPOCH_IN_LAMPORTS
        Cluster.DEVNET.endpoint -> RENT_PER_EPOCH_IN_LAMPORTS_DEV_NET
        else -> RENT_PER_EPOCH_IN_LAMPORTS
    }

    private companion object {
        const val MIN_ACCOUNT_DATA_SIZE = 0L
        const val SIGNATURE_PLACEHOLDER_LENGTH = 65

        const val ACCOUNT_METADATA_SIZE = 128L
        const val RENT_PER_EPOCH_IN_LAMPORTS = 19.055441478439427
        const val RENT_PER_EPOCH_IN_LAMPORTS_DEV_NET = 0.359375
    }
}