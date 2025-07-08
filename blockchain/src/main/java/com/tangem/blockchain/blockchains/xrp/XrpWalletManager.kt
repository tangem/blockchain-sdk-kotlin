package com.tangem.blockchain.blockchains.xrp

import android.util.Log
import com.tangem.blockchain.blockchains.xrp.network.XrpInfoResponse
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.common.trustlines.AssetRequirementsCondition
import com.tangem.blockchain.common.trustlines.AssetRequirementsManager
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.extensions.toSimpleFailure
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import java.math.BigDecimal
import java.util.Calendar

class XrpWalletManager(
    wallet: Wallet,
    private val transactionBuilder: XrpTransactionBuilder,
    private val networkProvider: XrpNetworkProvider,
) : WalletManager(wallet), ReserveAmountProvider, TransactionValidator, AssetRequirementsManager {

    override val currentHost: String
        get() = networkProvider.baseUrl

    private val blockchain = wallet.blockchain

    override suspend fun updateInternal() {
        when (val result = networkProvider.getInfo(wallet.address)) {
            is Result.Success -> updateWallet(result.data)
            is Result.Failure -> updateError(result.error)
        }
    }

    private fun updateWallet(response: XrpInfoResponse) {
        Log.d(this::class.java.simpleName, "Balance is ${response.balance}")

        wallet.setReserveValue(response.reserveTotal)
        transactionBuilder.minReserve = response.reserveBase
        transactionBuilder.reserveInc = response.reserveInc
        if (!response.accountFound) {
            updateError(BlockchainSdkError.AccountNotFound(response.reserveTotal))
            return
        }
        wallet.setCoinValue(response.balance - response.reserveTotal)
        transactionBuilder.sequence = response.sequence
        transactionBuilder.tokenBalances = response.tokenBalances
        cardTokens.forEach { token ->
            val tokenBalance = response.tokenBalances
                .find { "${it.currency}.${it.issuer}" == token.contractAddress }?.balance
                ?: 0.toBigDecimal()
            wallet.addTokenValue(tokenBalance, token)
        }

        if (response.hasUnconfirmed) {
            if (wallet.recentTransactions.isEmpty()) wallet.addTransactionDummy()
        } else {
            wallet.recentTransactions.forEach { it.status = TransactionStatus.Confirmed }
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
        val transactionHash = when (val buildResult = transactionBuilder.buildToSign(transactionData)) {
            is Result.Success -> buildResult.data
            is Result.Failure -> return Result.Failure(buildResult.error)
        }

        return when (val signerResponse = signer.sign(transactionHash, wallet.publicKey)) {
            is CompletionResult.Success -> {
                val transactionToSend = transactionBuilder.buildToSend(signerResponse.data)
                when (val sendResult = networkProvider.sendTransaction(transactionToSend)) {
                    is SimpleResult.Failure -> Result.Failure(sendResult.error)
                    SimpleResult.Success -> {
                        val hash = transactionBuilder.getTransactionHash()?.toHexString()
                        transactionData.hash = hash
                        wallet.addOutgoingTransaction(transactionData)
                        Result.Success(TransactionSendResult(hash ?: ""))
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
        return when (val result = networkProvider.getFee()) {
            is Result.Failure -> result
            is Result.Success -> Result.Success(
                TransactionFee.Choosable(
                    minimum = Fee.Common(Amount(result.data.minimalFee, blockchain)),
                    normal = Fee.Common(Amount(result.data.normalFee, blockchain)),
                    priority = Fee.Common(Amount(result.data.priorityFee, blockchain)),
                ),
            )
        }
    }

    override fun getReserveAmount(): BigDecimal = transactionBuilder.minReserve

    override suspend fun isAccountFunded(destinationAddress: String): Boolean {
        return networkProvider.checkIsAccountCreated(destinationAddress)
    }

    override suspend fun validate(transactionData: TransactionData): kotlin.Result<Unit> {
        transactionData.requireUncompiled()

        val destinationTag = (transactionData.extras as? XrpTransactionBuilder.XrpTransactionExtras)?.destinationTag
        if (destinationTag == null && networkProvider.checkDestinationTagRequired(transactionData.destinationAddress)) {
            return kotlin.Result.failure(BlockchainSdkError.DestinationTagRequired)
        }
        return kotlin.Result.success(Unit)
    }

    override suspend fun requirementsCondition(currencyType: CryptoCurrencyType): AssetRequirementsCondition? {
        if (!assetRequiresAssociation(currencyType)) return null

        return when (currencyType) {
            is CryptoCurrencyType.Coin -> null
            is CryptoCurrencyType.Token -> {
                AssetRequirementsCondition.RequiredTrustline(
                    blockchain = Blockchain.XRP,
                    amount = Amount(blockchain = Blockchain.XRP, value = transactionBuilder.reserveInc),
                )
            }
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
            coinAmount = coinAmount,
        ).successOr { return it.toSimpleFailure() }

        return when (val signerResponse = signer.sign(hash, wallet.publicKey)) {
            is CompletionResult.Success -> {
                val transactionToSend = transactionBuilder.buildToSend(signerResponse.data)
                when (val sendResult = networkProvider.sendTransaction(transactionToSend)) {
                    is SimpleResult.Failure -> SimpleResult.Failure(sendResult.error)
                    SimpleResult.Success -> {
                        val transactionData = transactionData
                            .copy(hash = transactionBuilder.getTransactionHash()?.toHexString())
                        wallet.addOutgoingTransaction(transactionData)
                        updateInternal()
                        SimpleResult.Success
                    }
                }
            }
            is CompletionResult.Failure -> SimpleResult.fromTangemSdkError(signerResponse.error)
        }
    }

    override suspend fun discardRequirements(currencyType: CryptoCurrencyType): SimpleResult {
        return SimpleResult.Success
    }

    private fun CryptoCurrencyType.Token.trustlineTxKey() = "trustline-${info.contractAddress}"

    private fun assetRequiresAssociation(currencyType: CryptoCurrencyType): Boolean {
        return when (currencyType) {
            is CryptoCurrencyType.Coin -> false
            is CryptoCurrencyType.Token -> {
                val haveUnconfirmedTrustline = wallet.recentTransactions.any {
                    currencyType.trustlineTxKey() == it.destinationAddress && it.status == TransactionStatus.Unconfirmed
                }
                if (haveUnconfirmedTrustline) return false
                transactionBuilder.tokenBalances
                    .find { currencyType.info.contractAddress == "${it.currency}.${it.issuer}" } == null
            }
        }
    }
}