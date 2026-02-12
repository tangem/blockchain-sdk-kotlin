package com.tangem.blockchain.blockchains.xrp

import android.util.Log
import com.tangem.blockchain.blockchains.xrp.network.XrpInfoResponse
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkProvider
import com.tangem.blockchain.blockchains.xrp.network.XrpTokenBalance
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.datastorage.BlockchainSavedData
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
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
import com.tangem.common.extensions.toCompressedPublicKey
import com.tangem.common.extensions.toHexString
import java.math.BigDecimal
import java.util.Calendar

internal class XrpWalletManager(
    wallet: Wallet,
    private val transactionBuilder: XrpTransactionBuilder,
    private val networkProvider: XrpNetworkProvider,
    private val dataStorage: AdvancedDataStorage,
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

    private suspend fun updateWallet(response: XrpInfoResponse) {
        Log.d(this::class.java.simpleName, "Balance is ${response.balance}")

        wallet.setReserveValue(response.reserveTotal)
        transactionBuilder.minReserve = response.reserveBase
        transactionBuilder.reserveInc = response.reserveInc
        if (!response.accountFound) {
            updateError(BlockchainSdkError.AccountNotFound(response.reserveTotal))
            return
        }
        wallet.setCoinValue(response.balance - response.reserveTotal)
        storeIncompleteTokenTransaction(response.tokenBalances)
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
        val uncompiledTransaction = transactionData.requireUncompiled()
        if (uncompiledTransaction.amount.type is AmountType.Token) {
            val contractAddress = uncompiledTransaction.amount.type.token.contractAddress
            if (isNeedSetNoRippling(contractAddress)) {
                return sendTokenWithNoRippleSetup(uncompiledTransaction, contractAddress, signer)
            }
        }
        return performStandardSend(uncompiledTransaction, signer)
    }

    private suspend fun sendTokenWithNoRippleSetup(
        transactionData: TransactionData.Uncompiled,
        contractAddress: String,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        val sourceAddress = XrpAddressService.decodeXAddress(transactionData.sourceAddress)
            ?.address ?: transactionData.sourceAddress
        val sequence = networkProvider.getSequence(sourceAddress).successOr { return it }
        val fee = innerGetFee().successOr { return it }
        val trustSetData = TransactionData.Uncompiled(
            amount = Amount(blockchain = blockchain, value = 0.toBigDecimal()),
            fee = fee.normal,
            sourceAddress = transactionData.sourceAddress,
            destinationAddress = transactionData.sourceAddress,
            contractAddress = contractAddress,
        )

        val trustSetHash = transactionBuilder.buildToOpenTrustlineSign(
            transactionData = trustSetData,
            coinAmount = wallet.getCoinAmount(),
        ).successOr { return it }
        val (paymentHash, secondaryTx) = transactionBuilder.buildToSignWithNoRipple(
            transactionData = transactionData,
            sequenceOverride = sequence + 1,
        )
            .successOr { return it }
        val signatures = when (val signerResponse = signer.sign(listOf(trustSetHash, paymentHash), wallet.publicKey)) {
            is CompletionResult.Success -> signerResponse.data
            is CompletionResult.Failure -> return Result.fromTangemSdkError(signerResponse.error)
        }

        if (signatures.size != 2) {
            return Result.Failure(BlockchainSdkError.CustomError("Expected 2 signatures, got ${signatures.size}"))
        }

        val trustSetBlob = transactionBuilder.buildToSend(signatures[0])
        when (networkProvider.sendTransaction(trustSetBlob)) {
            is SimpleResult.Failure -> return Result.Failure(
                BlockchainSdkError.CustomError("Failed to send TrustSet transaction"),
            )
            SimpleResult.Success -> {
                updateTrustlineNoRippleStatus(contractAddress)
            }
        }
        val paymentBlob = transactionBuilder.buildSecondaryToSend(signatures[1], secondaryTx)
        return when (val sendResult = networkProvider.sendTransaction(paymentBlob)) {
            is SimpleResult.Failure -> Result.Failure(sendResult.error)
            SimpleResult.Success -> {
                val txHash = secondaryTx.hash.bytes().toHexString()
                wallet.addOutgoingTransaction(transactionData = transactionData, txHash = txHash)
                Result.Success(TransactionSendResult(txHash))
            }
        }
    }

    private suspend fun performStandardSend(
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
                        val txHash = transactionBuilder.getTransactionHash()?.toHexString().orEmpty()
                        wallet.addOutgoingTransaction(transactionData = transactionData, txHash = txHash)
                        Result.Success(TransactionSendResult(txHash))
                    }
                }
            }
            is CompletionResult.Failure -> Result.fromTangemSdkError(signerResponse.error)
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        return when (val result = networkProvider.getFee()) {
            is Result.Failure -> result
            is Result.Success -> {
                var minimalFee = result.data.minimalFee
                var normalFee = result.data.normalFee
                var priorityFee = result.data.priorityFee

                if (amount.type is AmountType.Token && isNeedSetNoRippling(amount.type.token.contractAddress)) {
                    minimalFee *= 2.toBigDecimal()
                    normalFee *= 2.toBigDecimal()
                    priorityFee *= 2.toBigDecimal()
                }
                Result.Success(
                    TransactionFee.Choosable(
                        minimum = Fee.Common(Amount(minimalFee, blockchain)),
                        normal = Fee.Common(Amount(normalFee, blockchain)),
                        priority = Fee.Common(Amount(priorityFee, blockchain)),
                    ),
                )
            }
        }
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
        val uncompiledTransaction = transactionData.requireUncompiled()

        val destinationTag =
            (uncompiledTransaction.extras as? XrpTransactionBuilder.XrpTransactionExtras)?.destinationTag
        if (destinationTag == null &&
            networkProvider.checkDestinationTagRequired(uncompiledTransaction.destinationAddress)
        ) {
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
                        val txHash = transactionBuilder.getTransactionHash()?.toHexString().orEmpty()
                        wallet.addOutgoingTransaction(transactionData = transactionData, txHash = txHash)
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

    private suspend fun assetRequiresAssociation(currencyType: CryptoCurrencyType): Boolean {
        return when (currencyType) {
            is CryptoCurrencyType.Coin -> false
            is CryptoCurrencyType.Token -> {
                val haveUnconfirmedTrustline = wallet.recentTransactions.any {
                    currencyType.trustlineTxKey() == it.destinationAddress && it.status == TransactionStatus.Unconfirmed
                }
                if (haveUnconfirmedTrustline) return false
                val createdTrustline = getCreatedTrustline()?.createdTrustline ?: return true
                val isTrustlineCreated = createdTrustline
                    .any { currencyType.info.contractAddress == it }
                return !isTrustlineCreated
            }
        }
    }

    private fun storeKey() =
        "trustline-${wallet.publicKey.blockchainKey.toCompressedPublicKey().toHexString()}-${wallet.address}"

    private fun XrpTokenBalance.canonicalContractAddress() =
        "${this.currency}${XrpTransactionBuilder.TANGEM_BACKEND_CONTRACT_ADDRESS_SEPARATOR}${this.issuer}"

    private suspend fun getCreatedTrustline(): BlockchainSavedData.Trustline? {
        return dataStorage.getOrNull(storeKey())
    }

    private suspend fun storeIncompleteTokenTransaction(data: Set<XrpTokenBalance>) {
        val trustlinesWithoutNoRipple = data
            .filterNot { it.noRipple }
            .mapTo(mutableSetOf()) { it.canonicalContractAddress() }

        dataStorage.store(
            key = storeKey(),
            value = BlockchainSavedData.Trustline(
                createdTrustline = data.mapTo(mutableSetOf()) { it.canonicalContractAddress() },
                trustlinesWithoutNoRipple = trustlinesWithoutNoRipple,
            ),
        )
    }

    private suspend fun isNeedSetNoRippling(contractAddress: String): Boolean {
        val trustlineData = getCreatedTrustline() ?: return false
        if (!trustlineData.createdTrustline.contains(contractAddress)) {
            return false
        }
        return trustlineData.trustlinesWithoutNoRipple.contains(contractAddress)
    }

    private suspend fun updateTrustlineNoRippleStatus(contractAddress: String) {
        val trustlineData = getCreatedTrustline() ?: return
        val updatedTrustlines = trustlineData.trustlinesWithoutNoRipple.toMutableSet()
        updatedTrustlines.remove(contractAddress)

        dataStorage.store(
            key = storeKey(),
            value = trustlineData.copy(trustlinesWithoutNoRipple = updatedTrustlines),
        )
    }
}