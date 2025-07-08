package com.tangem.blockchain.blockchains.stellar

import android.util.Log
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.common.trustlines.AssetRequirementsCondition
import com.tangem.blockchain.common.trustlines.AssetRequirementsManager
import com.tangem.blockchain.extensions.*
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import java.math.BigDecimal
import java.util.Calendar

class StellarWalletManager(
    wallet: Wallet,
    private val transactionBuilder: StellarTransactionBuilder,
    private val networkProvider: StellarNetworkProvider,
) : WalletManager(wallet),
    SignatureCountValidator,
    ReserveAmountProvider,
    TransactionValidator,
    AssetRequirementsManager {

    override val currentHost: String
        get() = networkProvider.baseUrl

    private val blockchain = wallet.blockchain

    private var baseFee = BASE_FEE
    private var baseReserve = BASE_RESERVE
    private var sequence = 0L
    private var tokenBalances: Set<StellarAssetBalance> = setOf()

    override suspend fun updateInternal() {
        when (val result = networkProvider.getInfo(wallet.address)) {
            is Result.Failure -> updateError(result.error)
            is Result.Success -> updateWallet(result.data)
        }
    }

    private fun updateWallet(data: StellarResponse) { // TODO: rework reserve
        val reserve = data.baseReserve * (2 + data.subEntryCount).toBigDecimal()
        wallet.setCoinValue(data.coinBalance - reserve)
        wallet.setReserveValue(reserve)
        sequence = data.sequence
        baseFee = data.baseFee
        baseReserve = data.baseReserve
        tokenBalances = data.tokenBalances
        transactionBuilder.minReserve = data.baseReserve * 2.toBigDecimal()

        cardTokens.forEach { token ->
            val tokenBalance = data.tokenBalances
                .find { "${it.symbol}-${it.issuer}" == token.contractAddress }?.balance
                ?: 0.toBigDecimal()
            wallet.addTokenValue(tokenBalance, token)
        }
        // only if no token(s) specified on manager creation or stored on card
        if (cardTokens.isEmpty()) updateUnplannedTokens(data.tokenBalances)

        updateRecentTransactions(data.recentTransactions)
    }

    private fun updateUnplannedTokens(balances: Set<StellarAssetBalance>) {
        balances.forEach {
            val token = Token(it.symbol, it.issuer, blockchain.decimals())
            wallet.addTokenValue(it.balance, token)
        }
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) throw error
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        val hash = transactionBuilder.buildToSign(transactionData, sequence).successOr {
            return Result.Failure(it.error)
        }

        return when (val signerResponse = signer.sign(hash, wallet.publicKey)) {
            is CompletionResult.Success -> {
                val transactionToSend = transactionBuilder.buildToSend(signerResponse.data)
                when (val sendResult = networkProvider.sendTransaction(transactionToSend)) {
                    is SimpleResult.Failure -> Result.Failure(sendResult.error)
                    SimpleResult.Success -> {
                        val txHash = transactionBuilder.getTransactionHash().toHexString()
                        transactionData.hash = txHash
                        wallet.addOutgoingTransaction(transactionData)
                        Result.Success(TransactionSendResult(txHash))
                    }
                }
            }
            is CompletionResult.Failure -> Result.fromTangemSdkError(signerResponse.error)
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        return innerGetFee()
    }

    private suspend fun innerGetFee(): Result<TransactionFee> {
        val feeStats = networkProvider.getFeeStats().successOr { return it }

        val minChargedFee = feeStats.feeCharged.mode.toBigDecimal().movePointLeft(blockchain.decimals())
        val normalChargedFee = feeStats.feeCharged.p80.toBigDecimal().movePointLeft(blockchain.decimals())
        val priorityChargedFee = feeStats.feeCharged.p99.toBigDecimal().movePointLeft(blockchain.decimals())

        return Result.Success(
            TransactionFee.Choosable(
                minimum = Fee.Common(Amount(minChargedFee, blockchain)),
                normal = Fee.Common(Amount(normalChargedFee, blockchain)),
                priority = Fee.Common(Amount(priorityChargedFee, blockchain)),
            ),
        )
    }

    override suspend fun validateSignatureCount(signedHashes: Int): SimpleResult {
        return when (val result = networkProvider.getSignatureCount(wallet.address)) {
            is Result.Success -> if (result.data == signedHashes) {
                SimpleResult.Success
            } else {
                SimpleResult.Failure(BlockchainSdkError.SignatureCountNotMatched)
            }
            is Result.Failure -> SimpleResult.Failure(result.error)
        }
    }

    override fun getReserveAmount(): BigDecimal = transactionBuilder.minReserve

    override suspend fun isAccountFunded(destinationAddress: String): Boolean {
        return when (val response = networkProvider.checkTargetAccount(destinationAddress, null)) {
            is Result.Success -> response.data.accountCreated
            is Result.Failure -> false
        }
    }

    override suspend fun validate(transactionData: TransactionData): kotlin.Result<Unit> {
        transactionData.requireUncompiled()

        val extras = transactionData.extras as? StellarTransactionExtras
        val hasMemo = extras?.memo?.hasNonEmptyMemo() ?: false

        val amountType = transactionData.amount.type
        val token = if (amountType is AmountType.Token) amountType.token else null

        val requiresMemo = networkProvider.checkTargetAccount(transactionData.destinationAddress, token)
            .map { it.requiresMemo }
            .successOr { false }

        if (!hasMemo && requiresMemo) {
            return kotlin.Result.failure(BlockchainSdkError.DestinationTagRequired)
        }
        return kotlin.Result.success(Unit)
    }

    override suspend fun requirementsCondition(currencyType: CryptoCurrencyType): AssetRequirementsCondition? {
        if (!assetRequiresAssociation(currencyType)) return null

        return when (currencyType) {
            is CryptoCurrencyType.Coin -> null
            is CryptoCurrencyType.Token ->
                AssetRequirementsCondition.RequiredTrustline(
                    blockchain = Blockchain.Stellar,
                    amount = Amount(blockchain = Blockchain.Stellar, value = baseReserve),
                )
        }
    }

    override suspend fun fulfillRequirements(
        currencyType: CryptoCurrencyType,
        signer: TransactionSigner,
    ): SimpleResult {
        if (!assetRequiresAssociation(currencyType) || currencyType !is CryptoCurrencyType.Token) {
            return SimpleResult.Success
        }
        val coinAmount = wallet.getCoinAmount()
        val fee = innerGetFee().successOr { return it.toSimpleFailure() }
        val transactionData = TransactionData.Uncompiled(
            contractAddress = currencyType.info.contractAddress,
            fee = fee.normal,
            amount = Amount(token = currencyType.info),
            sourceAddress = wallet.address,
            date = Calendar.getInstance(),
            destinationAddress = currencyType.trustlineTxKey(),
        )
        val hash = transactionBuilder.buildToOpenTrustlineSign(
            transactionData = transactionData,
            baseReserve = baseReserve,
            coinAmount = coinAmount,
            sequence = sequence,
        ).successOr { return it.toSimpleFailure() }

        return when (val signerResponse = signer.sign(hash, wallet.publicKey)) {
            is CompletionResult.Success -> {
                val transactionToSend = transactionBuilder.buildToSend(signerResponse.data)
                when (val sendResult = networkProvider.sendTransaction(transactionToSend)) {
                    is SimpleResult.Failure -> SimpleResult.Failure(sendResult.error)
                    SimpleResult.Success -> {
                        val transactionData = transactionData
                            .copy(hash = transactionBuilder.getTransactionHash().toHexString())
                        wallet.addOutgoingTransaction(transactionData)
                        updateInternal()
                        SimpleResult.Success
                    }
                }
            }
            is CompletionResult.Failure -> SimpleResult.fromTangemSdkError(signerResponse.error)
        }
    }

    private fun CryptoCurrencyType.Token.trustlineTxKey() = "trustline-${info.contractAddress}"

    override suspend fun discardRequirements(currencyType: CryptoCurrencyType): SimpleResult {
        return SimpleResult.Success
    }

    private fun assetRequiresAssociation(currencyType: CryptoCurrencyType): Boolean {
        return when (currencyType) {
            is CryptoCurrencyType.Coin -> false
            is CryptoCurrencyType.Token -> {
                val haveUnconfirmedTrustline = wallet.recentTransactions.any {
                    currencyType.trustlineTxKey() == it.destinationAddress && it.status == TransactionStatus.Unconfirmed
                }
                if (haveUnconfirmedTrustline) return false
                tokenBalances.find { currencyType.info.contractAddress == "${it.symbol}-${it.issuer}" } == null
            }
        }
    }

    companion object {
        val BASE_FEE = 0.00001.toBigDecimal()
        val BASE_RESERVE = 0.5.toBigDecimal()
    }
}