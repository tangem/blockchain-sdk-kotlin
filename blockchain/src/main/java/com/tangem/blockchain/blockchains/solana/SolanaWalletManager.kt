package com.tangem.blockchain.blockchains.solana

import android.util.Log
import com.tangem.blockchain.blockchains.solana.solanaj.core.Transaction
import com.tangem.blockchain.blockchains.solana.solanaj.rpc.RpcClient
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.BlockchainSdkError.NPError
import com.tangem.blockchain.common.BlockchainSdkError.Solana
import com.tangem.blockchain.common.BlockchainSdkError.UnsupportedOperation
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
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
import org.p2p.solanaj.programs.TokenProgram
import org.p2p.solanaj.rpc.types.TokenAccountInfo
import org.p2p.solanaj.rpc.types.config.Commitment
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Created by Anton Zhilenkov on 21/01/2022.
 */
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
            throw (it.error as BlockchainSdkError)
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
                    amount, Fee.Common(feeAmount), info.source, info.destination,
                    TransactionStatus.Unconfirmed, hash = it.signature
                )
            } else {
                null
            }
        }
        wallet.recentTransactions.addAll(newUnconfirmedTxData)
    }

    override fun createTransaction(
        amount: Amount,
        fee: Fee,
        destination: String
    ): TransactionData {
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

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner
    ): SimpleResult {
        return when (transactionData.amount.type) {
            AmountType.Coin -> sendCoin(transactionData, signer)
            is AmountType.Token -> sendSplToken(
                transactionData.amount.type.token,
                transactionData,
                signer
            )
            AmountType.Reserve -> SimpleResult.Failure(UnsupportedOperation())
        }
    }

    private suspend fun sendCoin(
        transactionData: TransactionData,
        signer: TransactionSigner
    ): SimpleResult {
        val recentBlockHash = multiNetworkProvider.performRequest { getRecentBlockhash()}.successOr {
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

    private suspend fun sendSplToken(
        token: Token,
        transactionData: TransactionData,
        signer: TransactionSigner
    ): SimpleResult {
        val sourcePubK = PublicKey(transactionData.sourceAddress)
        val destinationPubK = PublicKey(transactionData.destinationAddress)
        val mintPubKey = transactionData.contractAddress.guard {
            return SimpleResult.Failure(NPError("contractAddress"))
        }.let { PublicKey(it) }

        val sourceSplTokenPubK = PublicKey.associatedTokenAddress(
            sourcePubK,
            mintPubKey,
        ).guard {
            return SimpleResult.Failure(Solana.FailedToCreateAssociatedTokenAddress)
        }
        val destinationSplTokenInfo = multiNetworkProvider.performRequest {
            splAccountInfo(destinationPubK, mintPubKey)
        }.successOr {
            return SimpleResult.Failure(it.error)
        }
        if (sourceSplTokenPubK == destinationSplTokenInfo.associatedPubK) {
            return SimpleResult.Failure(Solana.SameSourceAndDestinationAddress)
        }

        val transaction = Transaction(accountPubK)
        if (!destinationSplTokenInfo.accountExist) {
            val createATokenInstruction =
                AssociatedTokenProgram.createSplAssociatedTokenAccountInstruction(
                    mintPubKey,
                    destinationSplTokenInfo.associatedPubK,
                    destinationPubK,
                    accountPubK,
                )
            transaction.addInstruction(createATokenInstruction)
        }

        val tokenAmountToSend =
            valueConverter.toLamports(token, transactionData.amount.value ?: BigDecimal.ZERO)

        val sendInstruction = TokenProgram.transfer(
            sourceSplTokenPubK,
            destinationSplTokenInfo.associatedPubK,
            tokenAmountToSend,
            accountPubK,
        )
        transaction.addInstruction(sendInstruction)
        val recentBlockHash = multiNetworkProvider.performRequest {
            getRecentBlockhash()
        }.successOr {
            return SimpleResult.Failure(it.error)
        }

        transaction.setRecentBlockHash(recentBlockHash)
        val signResult = signer.sign(transaction.getDataForSign(), wallet.publicKey).successOr {
            return SimpleResult.fromTangemSdkError(it.error)
        }

        transaction.addSignedDataSignature(signResult)
        val sendResult = multiNetworkProvider.performRequest {
            sendTransaction(transaction)
        }.successOr {
            return SimpleResult.Failure(it.error)
        }

        feeRentHolder.clear()
        transactionData.hash = sendResult
        wallet.addOutgoingTransaction(transactionData, false)

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

    private suspend fun getNetworkFee(): Result<BigDecimal> {
        return when (val result = multiNetworkProvider.performRequest {getFees()}) {
            is Result.Success -> {
                val feePerSignature = result.data.value.feeCalculator.lamportsPerSignature
                Result.Success(feePerSignature.toBigDecimal())
            }
            is Result.Failure -> result
        }
    }

    private suspend fun getAccountCreationRent(
        amount: Amount,
        destination: String
    ): Result<BigDecimal> {
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
                val isExist = multiNetworkProvider.performRequest { isSplTokenAccountExist(
                    account = destinationPubKey,
                    mint = PublicKey(amount.type.token.contractAddress)
                )}.successOr { return it }

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

    override suspend fun minimalBalanceForRentExemption(): Result<BigDecimal> {
        return when (val result = multiNetworkProvider.performRequest {
            minimalBalanceForRentExemption()
        }) {
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
    override fun toLamports(value: BigDecimal): Long =
        value.toLamports(Blockchain.Solana.decimals())

    override fun toLamports(token: Token, value: BigDecimal): Long =
        value.toLamports(token.decimals)
}

private fun Long.toSOL(): BigDecimal = this.toBigDecimal().toSOL()
private fun BigDecimal.toSOL(): BigDecimal =
    movePointLeft(Blockchain.Solana.decimals()).toSolanaDecimals()

private fun BigDecimal.toLamports(decimals: Int): Long =
    movePointRight(decimals).toSolanaDecimals().toLong()

private fun BigDecimal.toSolanaDecimals(): BigDecimal =
    this.setScale(Blockchain.Solana.decimals(), RoundingMode.HALF_UP)

private fun <T> Result<T>.toSimpleResult(): SimpleResult {
    return when (this) {
        is Result.Success -> SimpleResult.Success
        is Result.Failure -> SimpleResult.Failure(this.error)
    }
}

private fun List<TokenAccountInfo.Value>.retrieveLamportsBy(token: Token): Long? {
    return getSplTokenBy(token)?.account?.lamports
}

private fun List<TokenAccountInfo.Value>.getSplTokenBy(token: Token): TokenAccountInfo.Value? {
    return firstOrNull { it.pubkey == token.contractAddress }
}

private inline fun <T> CompletionResult<T>.successOr(failureClause: (CompletionResult.Failure<T>) -> Nothing): T {
    return when (this) {
        is CompletionResult.Success -> this.data
        is CompletionResult.Failure -> failureClause(this)
    }
}
