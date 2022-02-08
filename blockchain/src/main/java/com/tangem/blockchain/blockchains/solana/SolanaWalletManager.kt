package com.tangem.blockchain.blockchains.solana

import android.util.Log
import com.tangem.blockchain.blockchains.solana.solanaj.rpc.RpcClient
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.filterWith
import com.tangem.common.CompletionResult
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.programs.Program
import org.p2p.solanaj.rpc.types.TokenAccountInfo
import org.p2p.solanaj.rpc.types.config.Commitment
import java.math.BigDecimal
import java.math.RoundingMode

/**
[REDACTED_AUTHOR]
 */
class SolanaWalletManager(
    wallet: Wallet,
    jsonRpcProvider: RpcClient
) : WalletManager(wallet), TransactionSender, RentProvider {

    override val currentHost: String = jsonRpcProvider.endpoint

    private val publicKey: PublicKey = PublicKey(wallet.address)
    private val networkService = SolanaNetworkService(jsonRpcProvider)
    private val txBuilder = SolanaTransactionBuilder(ValueConverter())

    private val feeRentHolder = mutableMapOf<Amount, BigDecimal>()

    override suspend fun update() {
        when (val result = networkService.getInfo(publicKey)) {
            is Result.Success -> {
                val accountInfo = result.data
                wallet.setCoinValue(accountInfo.balance.toSOL())
                updateRecentTransactions()
                addToRecentTransactions(accountInfo.txsInProgress)

                cardTokens.forEach { cardToken ->
                    val tokenBalance = accountInfo.tokensByMint[cardToken.contractAddress]?.uiAmount
                        ?: BigDecimal.ZERO
                    wallet.addTokenValue(tokenBalance, cardToken)
                }
            }
            is Result.Failure -> {
                wallet.removeAllTokens()
                throw Exception(result.error)
            }
        }
    }

    private fun updateRecentTransactions() {
        val txSignatures = wallet.recentTransactions.mapNotNull { it.hash }
        val result = networkService.getSignatureStatuses(txSignatures)
        when (result) {
            is Result.Success -> {
                val confirmedTxData = mutableListOf<TransactionData>()
                val signaturesStatuses = txSignatures.zip(result.data.value)
                signaturesStatuses.forEach { pair ->
                    if (pair.second?.confirmationStatus == Commitment.FINALIZED.value) {
                        val foundRecentTxData = wallet.recentTransactions.firstOrNull { it.hash == pair.first }
                        foundRecentTxData?.let {
                            confirmedTxData.add(it.copy(status = TransactionStatus.Confirmed))
                        }
                    }
                }
                updateRecentTransactions(confirmedTxData)
            }
            is Result.Failure -> {
                Log.e(this.javaClass.simpleName, result.error.localizedMessage)
            }
        }
    }

    private fun addToRecentTransactions(txsInProgress: List<TransactionInfo>) {
        if (txsInProgress.isEmpty()) return

        val newTxsInProgress = txsInProgress.filterWith(wallet.recentTransactions) { a, b -> a.signature != b.hash }
        val newUnconfirmedTxData = newTxsInProgress.mapNotNull {
            if (it.instructions.isNotEmpty() && it.instructions[0].programId == Program.Id.system.toBase58()) {
                val info = it.instructions[0].parsed.info
                val amount = Amount(info.lamports.toSOL(), wallet.blockchain)
                val fee = Amount(it.fee.toSOL(), wallet.blockchain)
                TransactionData(amount, fee, info.source, info.destination, null,
                    TransactionStatus.Unconfirmed, hash = it.signature)
            } else {
                null
            }
        }
        wallet.recentTransactions.addAll(newUnconfirmedTxData)
    }

    override fun createTransaction(amount: Amount, fee: Amount, destination: String): TransactionData {
        val accountCreationRent = feeRentHolder[fee]
        feeRentHolder.clear()

        return if (accountCreationRent == null) {
            super.createTransaction(amount, fee, destination)
        } else {
            val newFee = fee.minus(accountCreationRent)
            val newAmount = amount.plus(accountCreationRent)
            super.createTransaction(newAmount, newFee, destination)
        }
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        return when (transactionData.amount.type) {
            AmountType.Coin -> sendCoin(transactionData, signer)
            is AmountType.Token -> sendToken(transactionData, signer)
            AmountType.Reserve -> SimpleResult.Failure(BlockchainSdkError.UnsupportedOperation())
        }
    }

    private suspend fun sendCoin(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val transaction = txBuilder.buildToSign(transactionData, networkService.getRecentBlockhash())
        return when (val signResult = signer.sign(transaction.getDataForSign(), wallet.cardId, wallet.publicKey)) {
            is CompletionResult.Success -> {
                transaction.addSignedDataSignature(signResult.data)
                val result = networkService.sendTransaction(transaction)
                when (result) {
                    is Result.Success -> {
                        transactionData.hash = result.data
                        wallet.addOutgoingTransaction(transactionData, false)
                        SimpleResult.Success
                    }
                    is Result.Failure -> SimpleResult.Failure(result.error)
                }
            }
            is CompletionResult.Failure -> SimpleResult.fromTangemSdkError(signResult.error)
        }
    }

    private fun sendToken(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        throw BlockchainSdkError.UnsupportedOperation()
//        val associatedSourceTokenAccountAddress = associatedTokenAddress(transactionData.sourceAddress, transactionData.contractAddress!!)
//        val amount = NSDecimalNumber(decimal: transaction.amount.value * token.decimalValue).uint64Value
//        val signer = SolanaTransactionSigner(transactionSigner: signer, cardId: wallet.cardId, walletPublicKey: wallet.publicKey)
//
//        return networkService.sendSplToken(
//            amount: amount,
//            sourceTokenAddress: associatedSourceTokenAccountAddress,
//            destinationAddress: transaction.destinationAddress,
//        token: token,
//        signer: signer
//        )
//        return SimpleResult.Success
    }

    /**
     * This is not a natural fee, as it may contain additional information about the amount that may be required
     * to open an account. Later, when creating a transaction, this amount will be deducted from fee and added
     * to the amount of the main transfer
     */
    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        val feeResult = getNetworkFee()
        val fee = (feeResult as? Result.Success)?.data
            ?: return feeResult as Result.Failure

        val creationRentResult = getAccountCreationRent(amount, destination)
        val accountCreationRent = (creationRentResult as? Result.Success)?.data?.toSOL()
            ?: return creationRentResult as Result.Failure

        var feeAmount = Amount(fee.toSOL(), wallet.blockchain)
        if (accountCreationRent > BigDecimal.ZERO) {
            feeAmount = feeAmount.plus(accountCreationRent)
            feeRentHolder[feeAmount] = accountCreationRent
        }

        return Result.Success(listOf(feeAmount))
    }

    private fun getNetworkFee(): Result<BigDecimal> {
        return when (val result = networkService.getFees()) {
            is Result.Success -> {
                val feePerSignature = result.data.value.feeCalculator.lamportsPerSignature
                Result.Success(feePerSignature.toBigDecimal())
            }
            is Result.Failure -> result
        }
    }

    private fun getAccountCreationRent(amount: Amount, destination: String): Result<BigDecimal> {
        return when (val result = networkService.isAccountExist(PublicKey(destination))) {
            is Result.Success -> {
                val accountCreationFee = if (result.data) {
                    BigDecimal.ZERO
                } else {
                    when (val minRentExemptResult = networkService.minimalBalanceForRentExemption()) {
                        is Result.Success -> {
                            if (amount.value!!.toLamports().toBigDecimal() >= minRentExemptResult.data) {
                                BigDecimal.ZERO
                            } else {
                                when (amount.type) {
                                    AmountType.Coin -> networkService.mainAccountCreationFee()
                                    is AmountType.Token -> minRentExemptResult.data
                                    AmountType.Reserve -> return Result.Failure(BlockchainSdkError.FailedToLoadFee)
                                }
                            }
                        }
                        is Result.Failure -> return Result.Failure(BlockchainSdkError.FailedToLoadFee)
                    }
                }
                Result.Success(accountCreationFee)
            }
            is Result.Failure -> result
        }
    }

    override suspend fun addToken(token: Token): Result<Amount> {
        throw BlockchainSdkError.UnsupportedOperation()

        if (!cardTokens.contains(token)) cardTokens.add(token)

        return addTokens(listOf(token))[0]
    }

    override suspend fun addTokens(tokens: List<Token>): List<Result<Amount>> {
        throw BlockchainSdkError.UnsupportedOperation()

        tokens.forEach { if (!cardTokens.contains(it)) cardTokens.add(it) }

        return when (val result = networkService.tokenAccountsInfo(publicKey)) {
            is Result.Success -> addTokenValue(tokens, result.data).map { Result.Success(it) }
            is Result.Failure -> tokens.map { Result.Failure(result.error) }
        }
    }

    private fun addTokenValue(
        tokens: List<Token>,
        loadedSPLTokens: List<TokenAccountInfo.Value>
    ): List<Amount> {
        return tokens.map { token ->
            val amountValue = loadedSPLTokens.retrieveLamportsBy(token)?.toSOL() ?: BigDecimal.ZERO
            wallet.addTokenValue(amountValue, token)
        }
    }

    override suspend fun minimalBalanceForRentExemption(): Result<BigDecimal> {
        return when (val result = networkService.minimalBalanceForRentExemption()) {
            is Result.Success -> Result.Success(result.data.toSOL())
            is Result.Failure -> result
        }
    }

    override suspend fun rentAmount(): BigDecimal {
        return networkService.accountRentFeeByEpoch().toSOL()
    }
}

interface SolanaValueConverter {
    fun toSol(value: BigDecimal): BigDecimal
    fun toSol(value: Long): BigDecimal
    fun toLamports(value: BigDecimal): Long
}

class ValueConverter : SolanaValueConverter {
    override fun toSol(value: BigDecimal): BigDecimal = value.toSOL()
    override fun toSol(value: Long): BigDecimal = value.toBigDecimal().toSOL()
    override fun toLamports(value: BigDecimal): Long = value.toLamports()
}

private fun Long.toSOL(): BigDecimal = this.toBigDecimal().toSOL()
private fun BigDecimal.toSOL(): BigDecimal = movePointLeft(Blockchain.Solana.decimals()).toSolanaDecimals()
private fun BigDecimal.toLamports(): Long = movePointRight(Blockchain.Solana.decimals()).toSolanaDecimals().toLong()
private fun BigDecimal.toSolanaDecimals(): BigDecimal = this.setScale(Blockchain.Solana.decimals(), RoundingMode.HALF_UP)

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
