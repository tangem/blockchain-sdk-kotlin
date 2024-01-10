package com.tangem.blockchain.blockchains.solana

import android.util.Log
import com.tangem.blockchain.blockchains.solana.solanaj.core.Transaction
import com.tangem.blockchain.blockchains.solana.solanaj.core.createAssociatedTokenAddress
import com.tangem.blockchain.blockchains.solana.solanaj.program.TokenProgramId
import com.tangem.blockchain.blockchains.solana.solanaj.program.createTransferCheckedInstruction
import com.tangem.blockchain.blockchains.solana.solanaj.rpc.RpcClient
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.BlockchainSdkError.NPError
import com.tangem.blockchain.common.BlockchainSdkError.Solana
import com.tangem.blockchain.common.BlockchainSdkError.UnsupportedOperation
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.filterWith
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.guard
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.programs.AssociatedTokenProgram
import org.p2p.solanaj.programs.Program
import org.p2p.solanaj.programs.SystemProgram
import org.p2p.solanaj.rpc.types.config.Commitment
import java.math.BigDecimal
import java.math.RoundingMode

/**
[REDACTED_AUTHOR]
 */
// FIXME: Refactor with wallet-core: [REDACTED_JIRA]
@Suppress("LargeClass")
class SolanaWalletManager(
    wallet: Wallet,
    providers: List<RpcClient>,
) : WalletManager(wallet), TransactionSender, RentProvider {

    private val accountPubK: PublicKey = PublicKey(wallet.address)
    private val networkServices = providers.map { SolanaNetworkService(it) }

    private val multiNetworkProvider: MultiNetworkProvider<SolanaNetworkService> =
        MultiNetworkProvider(networkServices)

    override val currentHost: String
        get() = multiNetworkProvider.currentProvider.baseUrl

    private val feeRentHolder = mutableMapOf<Fee, BigDecimal>()
    private val valueConverter = ValueConverter()

    override suspend fun updateInternal() {
        val accountInfo = multiNetworkProvider.performRequest {
            getMainAccountInfo(accountPubK)
        }.successOr {
            wallet.removeAllTokens()
            throw it.error as BlockchainSdkError
        }
        wallet.setCoinValue(valueConverter.toSol(accountInfo.balance))
        updateRecentTransactions()
        addToRecentTransactions(accountInfo.txsInProgress)

        cardTokens.forEach { cardToken ->
            val tokenBalance =
                accountInfo.tokensByMint[cardToken.contractAddress]?.uiAmount ?: BigDecimal.ZERO
            wallet.addTokenValue(tokenBalance, cardToken)
        }
    }

    private suspend fun updateRecentTransactions() {
        val txSignatures = wallet.recentTransactions.mapNotNull { it.hash }
        val signatureStatuses = multiNetworkProvider.performRequest {
            getSignatureStatuses(txSignatures)
        }.successOr {
            Log.e(this.javaClass.simpleName, it.error.customMessage)
            return
        }

        val confirmedTxData = mutableListOf<TransactionData>()
        val signaturesStatuses = txSignatures.zip(signatureStatuses.value)
        signaturesStatuses.forEach { pair ->
            if (pair.second?.confirmationStatus == Commitment.FINALIZED.value) {
                val foundRecentTxData =
                    wallet.recentTransactions.firstOrNull { it.hash == pair.first }
                foundRecentTxData?.let {
                    confirmedTxData.add(it.copy(status = TransactionStatus.Confirmed))
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
                val amount = Amount(valueConverter.toSol(info.lamports), wallet.blockchain)
                val feeAmount = Amount(valueConverter.toSol(it.fee), wallet.blockchain)
                TransactionData(
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

    override fun createTransaction(amount: Amount, fee: Fee, destination: String): TransactionData {
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

                AmountType.Reserve -> throw UnsupportedOperation()
            }
        }
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        return when (transactionData.amount.type) {
            AmountType.Coin -> sendCoin(transactionData, signer)
            is AmountType.Token -> sendToken(
                transactionData.amount.type.token,
                transactionData,
                signer,
            )

            AmountType.Reserve -> SimpleResult.Failure(UnsupportedOperation())
        }
    }

    private suspend fun sendCoin(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val recentBlockHash = multiNetworkProvider.performRequest { getRecentBlockhash() }.successOr {
            return SimpleResult.Failure(it.error)
        }
        val from = PublicKey(transactionData.sourceAddress)
        val to = PublicKey(transactionData.destinationAddress)
        val lamports = ValueConverter().toLamports(transactionData.amount.value ?: BigDecimal.ZERO)
        val transaction = Transaction(accountPubK)
        transaction.addInstruction(SystemProgram.transfer(from, to, lamports))
        transaction.setRecentBlockHash(recentBlockHash)

        val signResult = signer.sign(transaction.getDataForSign(), wallet.publicKey).successOr {
            return SimpleResult.fromTangemSdkError(it.error)
        }

        transaction.addSignedDataSignature(signResult)
        val result = multiNetworkProvider.performRequest {
            sendTransaction(transaction)
        }.successOr {
            return SimpleResult.Failure(it.error)
        }

        feeRentHolder.clear()
        transactionData.hash = result
        wallet.addOutgoingTransaction(transactionData, false)

        return SimpleResult.Success
    }

    private suspend fun sendToken(
        token: Token,
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): SimpleResult {
        val transaction = buildTokenTransaction(token, transactionData, signer)
            .successOr { return SimpleResult.Failure(it.error) }

        return sendTokenTransaction(transaction, transactionData)
    }

    private suspend fun buildTokenTransaction(
        token: Token,
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<Transaction> {
        val sourceAccount = PublicKey(transactionData.sourceAddress)
        val destinationAccount = PublicKey(transactionData.destinationAddress)
        val mint = transactionData.contractAddress
            .guard { return Result.Failure(NPError("contractAddress")) }
            .let(::PublicKey)

        val (sourceAssociatedAccount, tokenProgramId) = getAssociatedAccountAndTokenProgramId(
            account = sourceAccount,
            mint = mint,
        ).successOr { return it }

        val destinationAssociatedAccount = createAssociatedTokenAddress(
            account = destinationAccount,
            mint = mint,
            tokenProgramId = tokenProgramId,
        ).getOrElse {
            return Result.Failure(Solana.FailedToCreateAssociatedAccount)
        }

        if (sourceAssociatedAccount == destinationAssociatedAccount) {
            return Result.Failure(Solana.SameSourceAndDestinationAddress)
        }

        val transaction = Transaction(accountPubK).apply {
            addInstructions(
                tokenProgramId = tokenProgramId,
                mint = mint,
                destinationAssociatedAccount = destinationAssociatedAccount,
                destinationAccount = destinationAccount,
                sourceAssociatedAccount = sourceAssociatedAccount,
                token = token,
                transactionData = transactionData,
            ).successOr { return Result.Failure(it.error) }

            val recentBlockHash = multiNetworkProvider.performRequest {
                getRecentBlockhash()
            }.successOr {
                return it
            }
            setRecentBlockHash(recentBlockHash)

            val signResult = signer.sign(getDataForSign(), wallet.publicKey).successOr {
                return Result.fromTangemSdkError(it.error)
            }
            addSignedDataSignature(signResult)
        }

        return Result.Success(transaction)
    }

    @Suppress("LongParameterList")
    private suspend fun Transaction.addInstructions(
        tokenProgramId: TokenProgramId,
        mint: PublicKey,
        destinationAssociatedAccount: PublicKey,
        destinationAccount: PublicKey,
        sourceAssociatedAccount: PublicKey,
        token: Token,
        transactionData: TransactionData,
    ): SimpleResult {
        val isDestinationAccountExists = multiNetworkProvider.performRequest {
            isTokenAccountExist(destinationAssociatedAccount)
        }.successOr { return SimpleResult.Failure(it.error) }

        if (!isDestinationAccountExists) {
            val associatedTokenInstruction = AssociatedTokenProgram.createAssociatedTokenAccountInstruction(
                /* associatedProgramId = */ Program.Id.splAssociatedTokenAccount,
                /* programId = */ tokenProgramId.value,
                /* mint = */ mint,
                /* associatedAccount = */ destinationAssociatedAccount,
                /* owner = */ destinationAccount,
                /* payer = */ accountPubK,
            )
            addInstruction(associatedTokenInstruction)
        }

        val sendInstruction = createTransferCheckedInstruction(
            source = sourceAssociatedAccount,
            destination = destinationAssociatedAccount,
            amount = valueConverter.toLamports(
                token = token,
                value = transactionData.amount.value ?: BigDecimal.ZERO,
            ),
            owner = accountPubK,
            decimals = token.decimals.toByte(),
            tokenMint = mint,
            programId = tokenProgramId,
        )
        addInstruction(sendInstruction)

        return SimpleResult.Success
    }

    private suspend fun getAssociatedAccountAndTokenProgramId(
        account: PublicKey,
        mint: PublicKey,
    ): Result<Pair<PublicKey, TokenProgramId>> {
        val resultForTokenProgram = tryGetAssociatedTokenAddressAndTokenProgramId(
            account = account,
            mint = mint,
            programId = TokenProgramId.TOKEN,
        )

        return when (resultForTokenProgram) {
            is Result.Failure -> tryGetAssociatedTokenAddressAndTokenProgramId(
                account = account,
                mint = mint,
                programId = TokenProgramId.TOKEN_2022,
            )

            is Result.Success -> resultForTokenProgram
        }
    }

    private suspend fun tryGetAssociatedTokenAddressAndTokenProgramId(
        account: PublicKey,
        mint: PublicKey,
        programId: TokenProgramId,
    ): Result<Pair<PublicKey, TokenProgramId>> {
        val associatedTokenAddress = createAssociatedTokenAddress(
            account = account,
            mint = mint,
            tokenProgramId = programId,
        ).getOrElse {
            return Result.Failure(Solana.FailedToCreateAssociatedAccount)
        }

        val isTokenAccountExist = multiNetworkProvider.performRequest {
            isTokenAccountExist(associatedTokenAddress)
        }.successOr { return it }

        return if (isTokenAccountExist) {
            Result.Success(data = associatedTokenAddress to programId)
        } else {
            Result.Failure(Solana.FailedToCreateAssociatedAccount)
        }
    }

    private suspend fun sendTokenTransaction(
        transaction: Transaction,
        transactionData: TransactionData,
    ): SimpleResult {
        val sendResult = multiNetworkProvider.performRequest {
            sendTransaction(transaction)
        }.successOr {
            return SimpleResult.Failure(it.error)
        }

        feeRentHolder.clear()
        transactionData.hash = sendResult
        wallet.addOutgoingTransaction(transactionData, hashToLowercase = false)

        return SimpleResult.Success
    }

    /**
     * This is not a natural fee, as it may contain additional information about the amount that may be required
     * to open an account. Later, when creating a transaction, this amount will be deducted from fee and added
     * to the amount of the main transfer
     */
    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        feeRentHolder.clear()
        val fee = getNetworkFee().successOr { return it }
        val accountCreationRent =
            getAccountCreationRent(amount, destination).successOr { return it }.let {
                valueConverter.toSol(it)
            }

        var feeAmount = Fee.Common(Amount(valueConverter.toSol(fee), wallet.blockchain))
        if (accountCreationRent > BigDecimal.ZERO) {
            feeAmount = feeAmount.copy(amount = feeAmount.amount + accountCreationRent)
            feeRentHolder[feeAmount] = accountCreationRent
        }

        return Result.Success(TransactionFee.Single(feeAmount))
    }

    override suspend fun estimateFee(amount: Amount, destination: String): Result<TransactionFee> {
        val feeRawValue = getNetworkFee().successOr { return it }
        val feeAmount = Fee.Common(Amount(valueConverter.toSol(feeRawValue), wallet.blockchain))
        return Result.Success(TransactionFee.Single(feeAmount))
    }

    private suspend fun getNetworkFee(): Result<BigDecimal> {
        return when (val result = multiNetworkProvider.performRequest { getFees() }) {
            is Result.Success -> {
                val feePerSignature = result.data.value.feeCalculator.lamportsPerSignature
                Result.Success(feePerSignature.toBigDecimal())
            }

            is Result.Failure -> result
        }
    }

    private suspend fun getAccountCreationRent(amount: Amount, destination: String): Result<BigDecimal> {
        val amountValue = amount.value.guard {
            return Result.Failure(NPError("amountValue"))
        }
        val destinationPubKey = PublicKey(destination)

        val accountCreationFee = when (amount.type) {
            AmountType.Coin -> {
                val isExist =
                    multiNetworkProvider.performRequest {
                        isAccountExist(destinationPubKey)
                    }.successOr { return it }
                if (isExist) return Result.Success(BigDecimal.ZERO)
                val minRentExempt =
                    multiNetworkProvider.performRequest {
                        minimalBalanceForRentExemption()
                    }.successOr { return it }

                if (valueConverter.toLamports(amountValue).toBigDecimal() >= minRentExempt) {
                    BigDecimal.ZERO
                } else {
                    multiNetworkProvider.currentProvider.mainAccountCreationFee()
                }
            }

            is AmountType.Token -> {
                val isExist = isTokenAccountExist(
                    account = destinationPubKey,
                    mint = PublicKey(amount.type.token.contractAddress),
                ).successOr { return it }

                if (isExist) {
                    BigDecimal.ZERO
                } else {
                    multiNetworkProvider.performRequest {
                        tokenAccountCreationFee()
                    }.successOr { return it }
                }
            }

            AmountType.Reserve -> return Result.Failure(UnsupportedOperation())
        }

        return Result.Success(accountCreationFee)
    }

    private suspend fun isTokenAccountExist(account: PublicKey, mint: PublicKey): Result<Boolean> {
        val isTokenAccountExistInTokenProgram = isTokenAccountExist(account, mint, TokenProgramId.TOKEN)
            .successOr { return it }

        return if (isTokenAccountExistInTokenProgram) {
            Result.Success(data = true)
        } else {
            isTokenAccountExist(account, mint, TokenProgramId.TOKEN_2022)
        }
    }

    private suspend fun isTokenAccountExist(
        account: PublicKey,
        mint: PublicKey,
        programId: TokenProgramId,
    ): Result<Boolean> {
        val associatedTokenAddress = createAssociatedTokenAddress(
            account = account,
            mint = mint,
            tokenProgramId = programId,
        ).getOrElse {
            return Result.Failure(Solana.FailedToCreateAssociatedAccount)
        }

        return multiNetworkProvider.performRequest {
            isTokenAccountExist(associatedTokenAddress)
        }
    }

    override suspend fun minimalBalanceForRentExemption(): Result<BigDecimal> {
        return when (
            val result = multiNetworkProvider.performRequest {
                minimalBalanceForRentExemption()
            }
        ) {
            is Result.Success -> Result.Success(valueConverter.toSol(result.data))
            is Result.Failure -> result
        }
    }

    override suspend fun rentAmount(): BigDecimal {
        return valueConverter.toSol(multiNetworkProvider.currentProvider.accountRentFeeByEpoch())
    }
}

interface SolanaValueConverter {
    fun toSol(value: BigDecimal): BigDecimal
    fun toSol(value: Long): BigDecimal
    fun toLamports(value: BigDecimal): Long
    fun toLamports(token: Token, value: BigDecimal): Long
}

class ValueConverter : SolanaValueConverter {
    override fun toSol(value: BigDecimal): BigDecimal = value.toSOL()
    override fun toSol(value: Long): BigDecimal = value.toBigDecimal().toSOL()
    override fun toLamports(value: BigDecimal): Long = value.toLamports(Blockchain.Solana.decimals())

    override fun toLamports(token: Token, value: BigDecimal): Long = value.toLamports(token.decimals)
}

private fun Long.toSOL(): BigDecimal = this.toBigDecimal().toSOL()
private fun BigDecimal.toSOL(): BigDecimal = movePointLeft(Blockchain.Solana.decimals()).toSolanaDecimals()

private fun BigDecimal.toLamports(decimals: Int): Long = movePointRight(decimals).toSolanaDecimals().toLong()

private fun BigDecimal.toSolanaDecimals(): BigDecimal =
    this.setScale(Blockchain.Solana.decimals(), RoundingMode.HALF_UP)

private inline fun <T> CompletionResult<T>.successOr(failureClause: (CompletionResult.Failure<T>) -> Nothing): T {
    return when (this) {
        is CompletionResult.Success -> this.data
        is CompletionResult.Failure -> failureClause(this)
    }
}