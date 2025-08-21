package com.tangem.blockchain.blockchains.hedera

import android.util.Log
import com.hedera.hashgraph.sdk.AccountId
import com.hedera.hashgraph.sdk.Transaction
import com.hedera.hashgraph.sdk.TransactionId
import com.hedera.hashgraph.sdk.TransactionResponse
import com.tangem.blockchain.blockchains.hedera.models.HederaAccountBalance
import com.tangem.blockchain.blockchains.hedera.models.HederaAccountInfo
import com.tangem.blockchain.blockchains.hedera.models.HederaTransactionId
import com.tangem.blockchain.blockchains.hedera.models.TokenAssociation
import com.tangem.blockchain.blockchains.hedera.network.HederaNetworkService
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.datastorage.BlockchainSavedData
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.common.trustlines.AssetRequirementsCondition
import com.tangem.blockchain.common.trustlines.AssetRequirementsManager
import com.tangem.blockchain.extensions.*
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.toCompressedPublicKey
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar

internal class HederaWalletManager(
    wallet: Wallet,
    private val transactionBuilder: HederaTransactionBuilder,
    private val networkService: HederaNetworkService,
    private val dataStorage: AdvancedDataStorage,
    private val accountCreator: AccountCreator,
) : WalletManager(wallet), AssetRequirementsManager {

    private val blockchain = wallet.blockchain

    private var tokenAssociationFeeExchangeRate: BigDecimal? = null

    override val currentHost: String
        get() = networkService.baseUrl

    override suspend fun updateInternal() {
        when (val getAccountIdResult = getAccountId()) {
            is Result.Success -> updateAccountInfo(getAccountIdResult.data)
            is Result.Failure -> updateError(getAccountIdResult.error)
        }
    }

    private suspend fun updateAccountInfo(accountId: String) {
        val pendingTxs = wallet.recentTransactions
            .filter { it.status == TransactionStatus.Unconfirmed }
            .mapNotNullTo(hashSetOf()) { it.hash?.let { hash -> HederaTransactionId.fromRawStringId(hash) } }
        when (val balances = networkService.getAccountInfo(accountId, pendingTxs)) {
            is Result.Success -> updateWallet(accountId, balances.data)
            is Result.Failure -> updateError(balances.error)
        }
    }

    private suspend fun updateWallet(accountId: String, accountInfo: HederaAccountInfo) {
        val balance = accountInfo.balance
        Log.d(this::class.java.simpleName, "Balance is ${balance.hbarBalance}")
        val associatedTokens = balance.associatedTokens()

        // Preserve any newly associated tokens that haven't been confirmed on network yet
        val cachedData = dataStorage.getOrNull<BlockchainSavedData.Hedera>(wallet.publicKey)
        val pendingAssociatedTokens = cachedData?.associatedTokens ?: emptySet()
        val mergedAssociatedTokens = associatedTokens + pendingAssociatedTokens

        cacheData(accountId = accountId, associatedTokens = mergedAssociatedTokens)

        wallet.changeAmountValue(AmountType.Coin, balance.hbarBalance.movePointLeft(blockchain.decimals()))
        cardTokens.forEach { token ->
            val tokenBalance = balance.tokenBalances
                .find { token.contractAddress == it.contractAddress }
                ?.balance
                ?.movePointLeft(token.decimals)
                ?: BigDecimal.ZERO
            wallet.addTokenValue(tokenBalance, token)
        }
        accountInfo.pendingTxsInfo.forEach { txInfo ->
            wallet.recentTransactions.find { it.hash == txInfo.id.rawStringId }?.let { txData ->
                txData.status = if (txInfo.isPending) TransactionStatus.Unconfirmed else TransactionStatus.Confirmed
            }
        }
        requestExchangeRateIfNeeded(associatedTokens)
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) error("Error isn't BlockchainSdkError")
    }

    private suspend fun assetRequiresAssociation(currencyType: CryptoCurrencyType): Boolean {
        return when (currencyType) {
            is CryptoCurrencyType.Coin -> false
            is CryptoCurrencyType.Token -> {
                val cachedData = dataStorage.getOrNull<BlockchainSavedData.Hedera>(wallet.publicKey) ?: return false
                val associatedTokens = cachedData.associatedTokens
                !associatedTokens.contains(currencyType.info.contractAddress)
            }
        }
    }

    override suspend fun requirementsCondition(currencyType: CryptoCurrencyType): AssetRequirementsCondition? {
        if (!assetRequiresAssociation(currencyType)) return null

        return when (currencyType) {
            is CryptoCurrencyType.Coin -> null
            is CryptoCurrencyType.Token -> {
                val exchangeRate = tokenAssociationFeeExchangeRate
                if (exchangeRate == null) {
                    AssetRequirementsCondition.PaidTransaction
                } else {
                    val feeValue = exchangeRate * HBAR_TOKEN_ASSOCIATE_USD_COST
                    val feeAmount = Amount(blockchain = wallet.blockchain, value = feeValue)
                    AssetRequirementsCondition.PaidTransactionWithFee(
                        blockchain = blockchain,
                        feeAmount = feeAmount,
                    )
                }
            }
        }
    }

    override suspend fun fulfillRequirements(
        currencyType: CryptoCurrencyType,
        signer: TransactionSigner,
    ): SimpleResult {
        if (!assetRequiresAssociation(currencyType)) return SimpleResult.Success

        return when (currencyType) {
            is CryptoCurrencyType.Coin -> SimpleResult.Success
            is CryptoCurrencyType.Token -> {
                val transaction = transactionBuilder.buildTokenAssociationForSign(
                    tokenAssociation = TokenAssociation(
                        accountId = wallet.address,
                        contractAddress = currencyType.info.contractAddress,
                    ),
                )
                when (transaction) {
                    is Result.Failure -> transaction.toSimpleResult()
                    is Result.Success -> {
                        val sendResult = signAndSendTransaction(signer = signer, builtTransaction = transaction.data)
                        if (sendResult is Result.Success) {
                            wallet.addOutgoingTransaction(
                                transactionData = TransactionData.Uncompiled(
                                    amount = Amount(token = currencyType.info),
                                    fee = Fee.Common(Amount(blockchain = blockchain)),
                                    sourceAddress = wallet.address,
                                    destinationAddress = currencyType.info.contractAddress,
                                    date = Calendar.getInstance(),
                                    hash = HederaTransactionId
                                        .fromTransactionId(sendResult.data.transactionId).rawStringId,
                                ),
                            )

                            // Add new associated token to cache
                            val cachedData = dataStorage.getOrNull<BlockchainSavedData.Hedera>(wallet.publicKey)
                            val currentAssociatedTokens = cachedData?.associatedTokens ?: emptySet()
                            val updatedAssociatedTokens = currentAssociatedTokens + currencyType.info.contractAddress
                            cacheData(
                                accountId = cachedData?.accountId ?: wallet.address,
                                associatedTokens = updatedAssociatedTokens,
                            )
                        }
                        sendResult.toSimpleResult()
                    }
                }
            }
        }
    }

    override suspend fun discardRequirements(currencyType: CryptoCurrencyType): SimpleResult {
        return SimpleResult.Success
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        return when (val buildTransaction = transactionBuilder.buildToSign(transactionData)) {
            is Result.Failure -> Result.Failure(buildTransaction.error)
            is Result.Success -> {
                when (val sendResult = signAndSendTransaction(signer, buildTransaction.data)) {
                    is Result.Failure -> Result.Failure(sendResult.error)
                    is Result.Success -> {
                        transactionData.setTransactionHash(sendResult.data.transactionId)
                        wallet.addOutgoingTransaction(transactionData)
                        Result.Success(TransactionSendResult(transactionData.hash ?: ""))
                    }
                }
            }
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        val transferFeeBase = when (amount.type) {
            AmountType.Coin -> HBAR_TRANSFER_USD_COST
            is AmountType.Token -> HBAR_TOKEN_TRANSFER_USD_COST
            else -> return Result.Failure(BlockchainSdkError.FailedToLoadFee)
        }

        val customFeesInfo = (amount.type as? AmountType.Token)?.let {
            networkService.getTokensCustomFeesInfo(it.token.contractAddress)
                .successOr { return Result.Failure(BlockchainSdkError.FailedToLoadFee) }
        }

        return when (val usdExchangeRateResult = networkService.getUsdExchangeRate()) {
            is Result.Success -> {
                val exchangeRate = usdExchangeRateResult.data
                val isAccountExists = isAccountExist(destination).successOr { false }

                var feeBase = if (isAccountExists) {
                    transferFeeBase
                } else {
                    HBAR_CREATE_ACCOUNT_USD_COST
                }

                if (customFeesInfo?.hasTokenCustomFees == true) {
                    feeBase += HBAR_CUSTOM_FEE_TOKEN_TRANSFER_USD_COST
                }

                val additionalHBARFee = customFeesInfo?.additionalHBARFee ?: BigDecimal.ZERO

                val feeValue = exchangeRate * feeBase * MAX_FEE_MULTIPLIER + additionalHBARFee

                val roundedFee = feeValue.setScale(blockchain.decimals(), RoundingMode.UP)
                val feeAmount = Amount(roundedFee, blockchain)

                val fee = Fee.Hedera(
                    amount = feeAmount,
                    additionalHBARFee = customFeesInfo?.additionalHBARFee ?: BigDecimal.ZERO,
                )

                Result.Success(TransactionFee.Single(fee))
            }
            is Result.Failure -> {
                usdExchangeRateResult
            }
        }
    }

    private suspend fun <T : Transaction<T>> signAndSendTransaction(
        signer: TransactionSigner,
        builtTransaction: HederaBuiltTransaction<T>,
    ): Result<TransactionResponse> {
        return try {
            when (val signerResult = signer.sign(builtTransaction.signatures, wallet.publicKey)) {
                is CompletionResult.Success -> {
                    val transactionToSend = transactionBuilder.buildToSend(
                        transaction = builtTransaction.transaction,
                        signatures = signerResult.data,
                    )
                    networkService.sendTransaction(transactionToSend)
                }
                is CompletionResult.Failure -> Result.fromTangemSdkError(signerResult.error)
            }
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private suspend fun getAccountId(): Result<String> {
        return if (wallet.address.isBlank()) {
            return when (val idResult = retrieveAccountId()) {
                is Result.Success -> {
                    wallet.addresses = setOf(Address(idResult.data))

                    idResult
                }
                is Result.Failure -> idResult
            }
        } else {
            Result.Success(wallet.address)
        }
    }

    private suspend fun retrieveAccountId(): Result<String> {
        val cachedData = dataStorage.getOrNull<BlockchainSavedData.Hedera>(wallet.publicKey)
        val isCacheCleared = cachedData?.isCacheCleared ?: false
        return if (isCacheCleared) {
            val cachedAccountId = cachedData?.accountId
            if (cachedAccountId.isNullOrBlank()) fetchAndStoreAccountId() else Result.Success(cachedAccountId)
        } else {
            fetchAndStoreAccountId()
        }
    }

    private suspend fun fetchAndStoreAccountId(): Result<String> {
        val publicKey = wallet.publicKey.blockchainKey.toCompressedPublicKey()
        val accountIdResult = when (val getAccountIdResult = networkService.getAccountId(publicKey)) {
            is Result.Success -> getAccountIdResult
            is Result.Failure -> {
                if (getAccountIdResult.error is BlockchainSdkError.AccountNotFound) {
                    requestCreateAccount()
                } else {
                    getAccountIdResult
                }
            }
        }
        if (accountIdResult is Result.Success) {
            cacheData(accountId = accountIdResult.data)
        }

        return accountIdResult
    }

    private suspend fun cacheData(accountId: String, associatedTokens: Set<String> = emptySet()) {
        dataStorage.store(
            publicKey = wallet.publicKey,
            value = BlockchainSavedData.Hedera(
                accountId = accountId,
                associatedTokens = associatedTokens,
                isCacheCleared = true,
            ),
        )
    }

    /** Used to query the status of the `receiving` (`destination`) account. */
    private suspend fun isAccountExist(destinationAddress: String): Result<Boolean> {
        val destinationAccountId = try {
            AccountId.fromString(destinationAddress)
        } catch (e: Exception) {
            return Result.Success(false)
        }
        // Accounts with an account ID and/or EVM address are considered existing accounts
        val accountHasValidAccountIdOrEVMAddress =
            destinationAccountId.num.toInt() != 0 || destinationAccountId.evmAddress != null
        if (accountHasValidAccountIdOrEVMAddress) {
            return Result.Success(true)
        }
        val alias = destinationAccountId.aliasKey.guard { return Result.Success(false) }
        return networkService.getAccountId(alias.toBytesRaw()).fold(
            success = { return Result.Success(true) },
            failure = { return Result.Success(false) },
        )
    }

    private suspend fun requestCreateAccount(): Result<String> {
        return accountCreator.createAccount(
            blockchain = blockchain,
            walletPublicKey = wallet.publicKey
                .blockchainKey,
        )
    }

    private fun TransactionData.setTransactionHash(transactionId: TransactionId) {
        hash = HederaTransactionId.fromTransactionId(transactionId).rawStringId
    }

    /**
     * We need this method to pre-load exchange rate, which will be used in associate notification
     */
    private suspend fun requestExchangeRateIfNeeded(alreadyAssociatedTokens: Set<String>) {
        if (cardTokens.all { token -> alreadyAssociatedTokens.contains(token.contractAddress) }) {
            // All added tokens(from `cardTokens`) are already associated with the current account;
            // Therefore there is no point in requesting an exchange rate to calculate the token association fee.

            return
        }

        tokenAssociationFeeExchangeRate = networkService.getUsdExchangeRate().successOr { return }
    }

    private fun HederaAccountBalance.associatedTokens(): Set<String> =
        tokenBalances.mapTo(hashSetOf(), HederaAccountBalance.TokenBalance::contractAddress)

    private companion object {
        // https://docs.hedera.com/hedera/networks/mainnet/fees
        val HBAR_TRANSFER_USD_COST = BigDecimal("0.0001")
        val HBAR_CREATE_ACCOUNT_USD_COST = BigDecimal("0.05")
        val HBAR_TOKEN_ASSOCIATE_USD_COST = BigDecimal("0.05")
        val HBAR_TOKEN_TRANSFER_USD_COST = BigDecimal("0.001")
        val HBAR_CUSTOM_FEE_TOKEN_TRANSFER_USD_COST = BigDecimal("0.001")
        /**
         * Hedera fees are low, allow 10% safety margin to allow usage of not precise fee estimate
         */
        val MAX_FEE_MULTIPLIER = BigDecimal("1.1")
    }
}