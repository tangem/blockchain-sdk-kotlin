package com.tangem.blockchain.blockchains.hedera

import android.util.Log
import com.hedera.hashgraph.sdk.AccountId
import com.hedera.hashgraph.sdk.Transaction
import com.hedera.hashgraph.sdk.TransactionResponse
import com.tangem.blockchain.blockchains.hedera.models.HederaAccountBalance
import com.tangem.blockchain.blockchains.hedera.models.HederaAccountInfo
import com.tangem.blockchain.blockchains.hedera.models.HederaTokenType
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
import java.math.BigInteger
import java.math.RoundingMode
import java.util.Calendar

@Suppress("LargeClass")
internal class HederaWalletManager(
    wallet: Wallet,
    private val transactionBuilder: HederaTransactionBuilder,
    private val networkService: HederaNetworkService,
    private val dataStorage: AdvancedDataStorage,
    private val accountCreator: AccountCreator,
) : WalletManager(wallet), AssetRequirementsManager {

    private val blockchain = wallet.blockchain
    private val tokenAddressConverter = HederaTokenAddressConverter()

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
        val pendingAssociatedTokens = cachedData?.associatedTokens.orEmpty()
        val mergedAssociatedTokens = associatedTokens + pendingAssociatedTokens

        // Resolve EVM contract addresses and detect token types
        val tokenTypes = cachedData?.tokenTypes.orEmpty().toMutableMap()
        val tokenEvmAddresses = cachedData?.tokenEvmAddresses.orEmpty().toMutableMap()
        val resolvedContractAddresses = cachedData?.resolvedContractAddresses.orEmpty().toMutableMap()

        cardTokens.forEach { token ->
            resolveTokenTypeIfNeeded(
                token = token,
                tokenTypes = tokenTypes,
                tokenEvmAddresses = tokenEvmAddresses,
                resolvedContractAddresses = resolvedContractAddresses,
            )
        }

        cacheData(
            accountId = accountId,
            associatedTokens = mergedAssociatedTokens,
            tokenTypes = tokenTypes,
            tokenEvmAddresses = tokenEvmAddresses,
            resolvedContractAddresses = resolvedContractAddresses,
        )

        wallet.changeAmountValue(AmountType.Coin, balance.hbarBalance.movePointLeft(blockchain.decimals()))

        // Load balances per token type
        val ownerEvmAddress = HederaUtils.accountIdToEvmAddress(accountId)
        cardTokens.forEach { token ->
            val tokenBalance = loadTokenBalance(
                token = token,
                balance = balance,
                tokenEvmAddresses = tokenEvmAddresses,
                resolvedContractAddresses = resolvedContractAddresses,
                ownerEvmAddress = ownerEvmAddress,
            )
            wallet.addTokenValue(tokenBalance, token)
        }

        updatePendingTransactions(accountInfo)
        requestExchangeRateIfNeeded(associatedTokens)
    }

    private suspend fun resolveTokenTypeIfNeeded(
        token: Token,
        tokenTypes: MutableMap<String, String>,
        tokenEvmAddresses: MutableMap<String, String>,
        resolvedContractAddresses: MutableMap<String, String>,
    ) {
        val existingType = tokenTypes[token.contractAddress]
        if (existingType != null) {
            val isErc20 = existingType == HederaTokenType.ERC20.name
            if (!isErc20) return

            val hasTokenEvmAddress = !tokenEvmAddresses[token.contractAddress].isNullOrBlank()
            val shouldResolveContractAddress = token.contractAddress.startsWith("0x") ||
                token.contractAddress.startsWith("0X")
            val hasResolvedContractAddress = !resolvedContractAddresses[token.contractAddress].isNullOrBlank()

            if (hasTokenEvmAddress && (!shouldResolveContractAddress || hasResolvedContractAddress)) return
        }

        val contractAddress = resolvedContractAddresses[token.contractAddress] ?: token.contractAddress
        val normalizedAddress = runCatching { tokenAddressConverter.convertToTokenId(contractAddress) }.getOrElse {
            return
        }

        if (normalizedAddress != contractAddress) {
            resolvedContractAddresses[token.contractAddress] = normalizedAddress
        }

        if (normalizedAddress.startsWith("0x") || normalizedAddress.startsWith("0X")) {
            resolveEvmContractAddress(
                token = token,
                tokenTypes = tokenTypes,
                tokenEvmAddresses = tokenEvmAddresses,
                resolvedContractAddresses = resolvedContractAddresses,
                evmAddress = normalizedAddress,
            )
        } else {
            detectAndCacheTokenType(
                token = token,
                tokenTypes = tokenTypes,
                tokenEvmAddresses = tokenEvmAddresses,
                contractAddress = normalizedAddress,
            )
        }
    }

    private suspend fun resolveEvmContractAddress(
        token: Token,
        tokenTypes: MutableMap<String, String>,
        tokenEvmAddresses: MutableMap<String, String>,
        resolvedContractAddresses: MutableMap<String, String>,
        evmAddress: String,
    ) {
        when (val contractInfoResult = networkService.getContractInfo(evmAddress)) {
            is Result.Success -> {
                val info = contractInfoResult.data
                resolvedContractAddresses[token.contractAddress] = info.contractId
                tokenTypes[token.contractAddress] = HederaTokenType.ERC20.name
                tokenEvmAddresses[token.contractAddress] = info.evmAddress
            }
            is Result.Failure -> {
                // Avoid routing token through HTS flow on transient contract lookup failures
                tokenEvmAddresses[token.contractAddress] = evmAddress
                when (val fallbackType = networkService.detectTokenType(evmAddress)) {
                    is Result.Success -> tokenTypes[token.contractAddress] = fallbackType.data.name
                    is Result.Failure -> Log.w(
                        this::class.java.simpleName,
                        "Failed to resolve token type for $evmAddress: ${fallbackType.error.customMessage}",
                    )
                }
            }
        }
    }

    private suspend fun detectAndCacheTokenType(
        token: Token,
        tokenTypes: MutableMap<String, String>,
        tokenEvmAddresses: MutableMap<String, String>,
        contractAddress: String,
    ) {
        val typeResult = networkService.detectTokenType(contractAddress)
        if (typeResult is Result.Success) {
            tokenTypes[token.contractAddress] = typeResult.data.name
            if (typeResult.data == HederaTokenType.ERC20) {
                val evmResult = networkService.getContractEvmAddress(contractAddress)
                if (evmResult is Result.Success) {
                    tokenEvmAddresses[token.contractAddress] = evmResult.data
                }
            }
        }
    }

    private suspend fun loadTokenBalance(
        token: Token,
        balance: HederaAccountBalance,
        tokenEvmAddresses: Map<String, String>,
        resolvedContractAddresses: Map<String, String>,
        ownerEvmAddress: String,
    ): BigDecimal {
        val type = getTokenType(token.contractAddress)
        return if (type == HederaTokenType.ERC20) {
            loadErc20Balance(token, tokenEvmAddresses[token.contractAddress], ownerEvmAddress)
        } else {
            val resolvedAddress = resolvedContractAddresses[token.contractAddress] ?: token.contractAddress
            balance.tokenBalances
                .find { resolvedAddress == it.contractAddress }
                ?.balance
                ?.movePointLeft(token.decimals)
                ?: BigDecimal.ZERO
        }
    }

    private fun updatePendingTransactions(accountInfo: HederaAccountInfo) {
        accountInfo.pendingTxsInfo.forEach { txInfo ->
            wallet.recentTransactions.find { it.hash == txInfo.id.rawStringId }?.let { txData ->
                txData.status = if (txInfo.isPending) TransactionStatus.Unconfirmed else TransactionStatus.Confirmed
            }
        }
    }

    private suspend fun loadErc20Balance(token: Token, tokenEvmAddress: String?, ownerEvmAddress: String): BigDecimal {
        if (tokenEvmAddress == null) return BigDecimal.ZERO
        val result = networkService.getERC20Balance(tokenEvmAddress, ownerEvmAddress)
        return when (result) {
            is Result.Success -> result.data.toBigDecimal().movePointLeft(token.decimals)
            is Result.Failure -> BigDecimal.ZERO
        }
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error !is BlockchainSdkError) error("Error isn't BlockchainSdkError")
    }

    private suspend fun getTokenType(contractAddress: String): HederaTokenType {
        val cachedData = dataStorage.getOrNull<BlockchainSavedData.Hedera>(wallet.publicKey)
        val typeName = cachedData?.tokenTypes?.get(contractAddress)
        return try {
            if (typeName == null) return detectAndStoreTokenType(contractAddress)

            val cachedType = HederaTokenType.valueOf(typeName)
            if (cachedType != HederaTokenType.ERC20) return cachedType

            val hasTokenEvmAddress = !cachedData.tokenEvmAddresses[contractAddress].isNullOrBlank()
            if (hasTokenEvmAddress) {
                HederaTokenType.ERC20
            } else {
                detectAndStoreTokenType(contractAddress)
            }
        } catch (_: Exception) {
            detectAndStoreTokenType(contractAddress)
        }
    }

    /**
     * Get the resolved contract address (0.0.X format) for a token.
     * If the token's contractAddress was originally an EVM address, the resolved 0.0.X address is returned.
     */
    private suspend fun getResolvedContractAddress(contractAddress: String): String {
        val cachedData = dataStorage.getOrNull<BlockchainSavedData.Hedera>(wallet.publicKey)
        return cachedData?.resolvedContractAddresses?.get(contractAddress) ?: contractAddress
    }

    private suspend fun assetRequiresAssociation(currencyType: CryptoCurrencyType): Boolean {
        return when (currencyType) {
            is CryptoCurrencyType.Coin -> false
            is CryptoCurrencyType.Token -> {
                // ERC20 tokens don't need association
                if (getTokenType(currencyType.info.contractAddress) == HederaTokenType.ERC20) return false

                val cachedData = dataStorage.getOrNull<BlockchainSavedData.Hedera>(wallet.publicKey) ?: return false
                val associatedTokens = cachedData.associatedTokens
                val resolvedContractAddress = getResolvedContractAddress(currencyType.info.contractAddress)
                !associatedTokens.contains(resolvedContractAddress)
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
                val resolvedContractAddress = getResolvedContractAddress(currencyType.info.contractAddress)
                val transaction = transactionBuilder.buildTokenAssociationForSign(
                    tokenAssociation = TokenAssociation(
                        accountId = wallet.address,
                        contractAddress = resolvedContractAddress,
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
                                ),
                                txHash = HederaTransactionId
                                    .fromTransactionId(sendResult.data.transactionId).rawStringId,
                            )

                            // Add new associated token to cache
                            val cachedData = dataStorage.getOrNull<BlockchainSavedData.Hedera>(wallet.publicKey)
                            val currentAssociatedTokens = cachedData?.associatedTokens ?: emptySet()
                            val updatedAssociatedTokens = currentAssociatedTokens + resolvedContractAddress
                            cacheData(
                                accountId = cachedData?.accountId ?: wallet.address,
                                associatedTokens = updatedAssociatedTokens,
                                tokenTypes = cachedData?.tokenTypes.orEmpty(),
                                tokenEvmAddresses = cachedData?.tokenEvmAddresses.orEmpty(),
                                resolvedContractAddresses = cachedData?.resolvedContractAddresses.orEmpty(),
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
        val uncompiled = transactionData as? TransactionData.Uncompiled
            ?: return Result.Failure(BlockchainSdkError.CustomError("Expected uncompiled transaction"))

        val tokenType = (uncompiled.amount.type as? AmountType.Token)?.let {
            getTokenType(it.token.contractAddress)
        }

        // For ERC20 tokens, we need the recipient's EVM address, gasLimit, and resolved contract address
        val erc20Params = if (tokenType == HederaTokenType.ERC20) {
            buildErc20TransferParams(uncompiled).successOr { return Result.Failure(it.error) }
        } else {
            null
        }

        val buildTransaction = if (erc20Params != null) {
            transactionBuilder.buildErc20ToSign(
                transactionData = transactionData,
                erc20Params = erc20Params,
            )
        } else {
            transactionBuilder.buildHtsToSign(transactionData)
        }

        return when (buildTransaction) {
            is Result.Failure -> Result.Failure(buildTransaction.error)
            is Result.Success -> {
                when (val sendResult = signAndSendTransaction(signer, buildTransaction.data)) {
                    is Result.Failure -> Result.Failure(sendResult.error)
                    is Result.Success -> {
                        val txHash = HederaTransactionId.fromTransactionId(sendResult.data.transactionId).rawStringId
                        wallet.addOutgoingTransaction(transactionData = transactionData, txHash = txHash)
                        Result.Success(TransactionSendResult(txHash))
                    }
                }
            }
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        val tokenType = (amount.type as? AmountType.Token)?.let { getTokenType(it.token.contractAddress) }

        return if (tokenType == HederaTokenType.ERC20) {
            getErc20Fee(amount, destination)
        } else {
            getHtsFee(amount, destination)
        }
    }

    private suspend fun getHtsFee(amount: Amount, destination: String): Result<TransactionFee> {
        val transferFeeBase = when (amount.type) {
            AmountType.Coin -> HBAR_TRANSFER_USD_COST
            is AmountType.Token -> HBAR_TOKEN_TRANSFER_USD_COST
            else -> return Result.Failure(BlockchainSdkError.FailedToLoadFee)
        }

        val customFeesInfo = (amount.type as? AmountType.Token)?.let { tokenType ->
            val resolved = getResolvedContractAddress(tokenType.token.contractAddress)
            networkService.getTokensCustomFeesInfo(resolved)
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
                    additionalHBARFee = additionalHBARFee,
                )

                Result.Success(TransactionFee.Single(fee))
            }
            is Result.Failure -> {
                usdExchangeRateResult
            }
        }
    }

    private suspend fun getErc20Fee(amount: Amount, destination: String): Result<TransactionFee> {
        val recipientEvmAddress = getRecipientEvmAddress(destination)
            .successOr { return Result.Failure(BlockchainSdkError.FailedToLoadFee) }

        val uncompiled = TransactionData.Uncompiled(
            amount = amount,
            fee = Fee.Common(amount = Amount(blockchain = blockchain)),
            sourceAddress = wallet.address,
            destinationAddress = destination,
            date = Calendar.getInstance(),
        )

        val gasLimit = estimateErc20GasLimit(
            transactionData = uncompiled,
            recipientEvmAddress = recipientEvmAddress,
        ).successOr { return Result.Failure(BlockchainSdkError.FailedToLoadFee) }

        val gasPriceResult = networkService.getContractCallGasPrice()
        val gasPrice = gasPriceResult.successOr { return Result.Failure(BlockchainSdkError.FailedToLoadFee) }

        // fee = gasLimit * gasPrice, in tinybars. Convert to HBAR (8 decimals).
        val feeInTinybars = gasLimit * gasPrice
        val feeValue = feeInTinybars.toBigDecimal().movePointLeft(blockchain.decimals())
        val roundedFee = feeValue.setScale(blockchain.decimals(), RoundingMode.UP)
        val feeAmount = Amount(roundedFee, blockchain)

        val fee = Fee.Hedera(
            amount = feeAmount,
            additionalHBARFee = BigDecimal.ZERO,
        )

        return Result.Success(TransactionFee.Single(fee))
    }

    private suspend fun buildErc20TransferParams(
        uncompiled: TransactionData.Uncompiled,
    ): Result<HederaTransactionBuilder.Erc20TransferParams> {
        val token = (uncompiled.amount.type as AmountType.Token).token
        val resolvedContractAddress = resolveAndCacheContractAddressForErc20(token.contractAddress)
            .successOr { return Result.Failure(it.error) }
        val recipientEvmAddress = getRecipientEvmAddress(uncompiled.destinationAddress)
            .successOr { return Result.Failure(it.error) }
        val gasLimit = estimateErc20GasLimit(
            transactionData = uncompiled,
            recipientEvmAddress = recipientEvmAddress,
        ).successOr { return Result.Failure(it.error) }

        return Result.Success(
            HederaTransactionBuilder.Erc20TransferParams(
                recipientEvmAddress = recipientEvmAddress,
                gasLimit = gasLimit,
                resolvedContractAddress = resolvedContractAddress,
            ),
        )
    }

    private suspend fun getRecipientEvmAddress(destination: String): Result<String> {
        // If destination is already an EVM address
        if (destination.startsWith("0x") || destination.startsWith("0X")) {
            if (!HederaUtils.isValidEvmAddress(destination)) {
                return Result.Failure(BlockchainSdkError.CustomError("Invalid EVM destination address"))
            }
            return Result.Success(destination)
        }
        // Convert account ID to EVM address via network call
        return when (val evmAddressResult = networkService.getAccountEvmAddress(destination)) {
            is Result.Success -> {
                if (HederaUtils.isValidEvmAddress(evmAddressResult.data)) {
                    Result.Success(evmAddressResult.data)
                } else {
                    Result.Failure(BlockchainSdkError.CustomError("Invalid EVM address from account details"))
                }
            }
            is Result.Failure -> evmAddressResult
        }
    }

    private suspend fun estimateErc20GasLimit(
        transactionData: TransactionData.Uncompiled,
        recipientEvmAddress: String,
    ): Result<BigInteger> {
        val token = (transactionData.amount.type as? AmountType.Token)?.token
            ?: return Result.Failure(BlockchainSdkError.FailedToLoadFee)

        val cachedData = dataStorage.getOrNull<BlockchainSavedData.Hedera>(wallet.publicKey)
        val tokenEvmAddress = cachedData?.tokenEvmAddresses?.get(token.contractAddress)
            ?: return Result.Failure(BlockchainSdkError.FailedToLoadFee)

        val ownerEvmAddress = HederaUtils.accountIdToEvmAddress(wallet.address)

        val transferAmount = runCatching { transactionData.amount.longValue }.getOrElse {
            return Result.Failure(BlockchainSdkError.NPError("amount.longValue"))
        }

        return networkService.estimateERC20GasLimit(
            fromEvmAddress = ownerEvmAddress,
            tokenEvmAddress = tokenEvmAddress,
            recipientEvmAddress = recipientEvmAddress,
            amount = BigInteger.valueOf(transferAmount),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : Transaction<T>> signAndSendTransaction(
        signer: TransactionSigner,
        builtTransaction: HederaBuiltTransaction<out Transaction<*>>,
    ): Result<TransactionResponse> {
        return try {
            when (val signerResult = signer.sign(builtTransaction.signatures, wallet.publicKey)) {
                is CompletionResult.Success -> {
                    val transactionToSend = transactionBuilder.buildToSend(
                        transaction = builtTransaction.transaction as T,
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
            val existingData = dataStorage.getOrNull<BlockchainSavedData.Hedera>(wallet.publicKey)
            cacheData(
                accountId = accountIdResult.data,
                associatedTokens = existingData?.associatedTokens.orEmpty(),
                tokenTypes = existingData?.tokenTypes.orEmpty(),
                tokenEvmAddresses = existingData?.tokenEvmAddresses.orEmpty(),
                resolvedContractAddresses = existingData?.resolvedContractAddresses.orEmpty(),
            )
        }

        return accountIdResult
    }

    private suspend fun cacheData(
        accountId: String,
        associatedTokens: Set<String> = emptySet(),
        tokenTypes: Map<String, String> = emptyMap(),
        tokenEvmAddresses: Map<String, String> = emptyMap(),
        resolvedContractAddresses: Map<String, String> = emptyMap(),
    ) {
        dataStorage.store(
            publicKey = wallet.publicKey,
            value = BlockchainSavedData.Hedera(
                accountId = accountId,
                associatedTokens = associatedTokens,
                isCacheCleared = true,
                tokenTypes = tokenTypes,
                tokenEvmAddresses = tokenEvmAddresses,
                resolvedContractAddresses = resolvedContractAddresses,
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

    /**
     * We need this method to pre-load exchange rate, which will be used in associate notification
     */
    private suspend fun requestExchangeRateIfNeeded(alreadyAssociatedTokens: Set<String>) {
        // Only check HTS tokens for association
        val cachedData = dataStorage.getOrNull<BlockchainSavedData.Hedera>(wallet.publicKey)
        val htsTokens = cardTokens.filter { token ->
            val typeName = cachedData?.tokenTypes?.get(token.contractAddress)
            typeName == null || typeName == HederaTokenType.HTS.name
        }

        var areAllHtsAssociated = true
        for (token in htsTokens) {
            val resolvedAddress = getResolvedContractAddress(token.contractAddress)
            if (!alreadyAssociatedTokens.contains(resolvedAddress)) {
                areAllHtsAssociated = false
                break
            }
        }
        if (areAllHtsAssociated) {
            return
        }

        tokenAssociationFeeExchangeRate = networkService.getUsdExchangeRate().successOr { return }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private suspend fun detectAndStoreTokenType(contractAddress: String): HederaTokenType {
        val cachedData = dataStorage.getOrNull<BlockchainSavedData.Hedera>(wallet.publicKey)
        val tokenTypes = cachedData?.tokenTypes.orEmpty().toMutableMap()
        val tokenEvmAddresses = cachedData?.tokenEvmAddresses.orEmpty().toMutableMap()
        val resolvedContractAddresses = cachedData?.resolvedContractAddresses.orEmpty().toMutableMap()
        val previouslyCachedType = runCatching {
            tokenTypes[contractAddress]?.let(HederaTokenType::valueOf)
        }.getOrNull()

        val inputAddress = resolvedContractAddresses[contractAddress] ?: contractAddress
        val normalizedAddress = runCatching { tokenAddressConverter.convertToTokenId(inputAddress) }.getOrElse {
            return previouslyCachedType ?: HederaTokenType.HTS
        }

        var detectionAddress = normalizedAddress
        if (normalizedAddress.startsWith("0x") || normalizedAddress.startsWith("0X")) {
            when (val contractInfo = networkService.getContractInfo(normalizedAddress)) {
                is Result.Success -> {
                    detectionAddress = contractInfo.data.contractId
                    resolvedContractAddresses[contractAddress] = contractInfo.data.contractId
                    tokenEvmAddresses[contractAddress] = contractInfo.data.evmAddress
                    tokenTypes[contractAddress] = HederaTokenType.ERC20.name
                    cacheTokenMetadata(
                        cachedData = cachedData,
                        tokenTypes = tokenTypes,
                        tokenEvmAddresses = tokenEvmAddresses,
                        resolvedContractAddresses = resolvedContractAddresses,
                    )
                    return HederaTokenType.ERC20
                }
                is Result.Failure -> {
                    tokenEvmAddresses[contractAddress] = normalizedAddress
                    if (HederaUtils.evmAddressToAccountId(normalizedAddress) == null) {
                        tokenTypes[contractAddress] = HederaTokenType.ERC20.name
                        cacheTokenMetadata(
                            cachedData = cachedData,
                            tokenTypes = tokenTypes,
                            tokenEvmAddresses = tokenEvmAddresses,
                            resolvedContractAddresses = resolvedContractAddresses,
                        )
                        return HederaTokenType.ERC20
                    }
                }
            }
        } else if (normalizedAddress != inputAddress) {
            resolvedContractAddresses[contractAddress] = normalizedAddress
        }

        val detectedTypeResult = networkService.detectTokenType(detectionAddress)
        val detectedType = when (detectedTypeResult) {
            is Result.Success -> detectedTypeResult.data
            is Result.Failure -> {
                val fallbackType = if (previouslyCachedType == HederaTokenType.ERC20) {
                    HederaTokenType.ERC20
                } else if (
                    detectionAddress.startsWith("0x") || detectionAddress.startsWith("0X")
                ) {
                    HederaTokenType.ERC20
                } else {
                    HederaTokenType.HTS
                }
                tokenTypes[contractAddress] = fallbackType.name
                cacheTokenMetadata(
                    cachedData = cachedData,
                    tokenTypes = tokenTypes,
                    tokenEvmAddresses = tokenEvmAddresses,
                    resolvedContractAddresses = resolvedContractAddresses,
                )
                return fallbackType
            }
        }
        tokenTypes[contractAddress] = detectedType.name

        if (detectedType == HederaTokenType.ERC20 && tokenEvmAddresses[contractAddress] == null) {
            val evmResult = networkService.getContractEvmAddress(detectionAddress)
            if (evmResult is Result.Success) {
                tokenEvmAddresses[contractAddress] = evmResult.data
            }
        }

        cacheTokenMetadata(
            cachedData = cachedData,
            tokenTypes = tokenTypes,
            tokenEvmAddresses = tokenEvmAddresses,
            resolvedContractAddresses = resolvedContractAddresses,
        )
        return detectedType
    }

    private suspend fun resolveAndCacheContractAddressForErc20(contractAddress: String): Result<String> {
        val cachedData = dataStorage.getOrNull<BlockchainSavedData.Hedera>(wallet.publicKey)
        val resolvedFromCache = cachedData?.resolvedContractAddresses?.get(contractAddress)
        if (!resolvedFromCache.isNullOrBlank()) return Result.Success(resolvedFromCache)

        val evmAddress = cachedData?.tokenEvmAddresses?.get(contractAddress)
            ?: if (contractAddress.startsWith("0x") || contractAddress.startsWith("0X")) {
                contractAddress
            } else {
                return Result.Failure(BlockchainSdkError.FailedToLoadFee)
            }

        val contractInfo = networkService.getContractInfo(evmAddress).successOr { return Result.Failure(it.error) }

        val tokenTypes = cachedData?.tokenTypes.orEmpty().toMutableMap().apply {
            put(contractAddress, HederaTokenType.ERC20.name)
        }
        val tokenEvmAddresses = cachedData?.tokenEvmAddresses.orEmpty().toMutableMap().apply {
            put(contractAddress, contractInfo.evmAddress)
        }
        val resolvedContractAddresses = cachedData?.resolvedContractAddresses.orEmpty().toMutableMap().apply {
            put(contractAddress, contractInfo.contractId)
        }

        cacheTokenMetadata(
            cachedData = cachedData,
            tokenTypes = tokenTypes,
            tokenEvmAddresses = tokenEvmAddresses,
            resolvedContractAddresses = resolvedContractAddresses,
        )
        return Result.Success(contractInfo.contractId)
    }

    private suspend fun cacheTokenMetadata(
        cachedData: BlockchainSavedData.Hedera?,
        tokenTypes: Map<String, String>,
        tokenEvmAddresses: Map<String, String>,
        resolvedContractAddresses: Map<String, String>,
    ) {
        val accountId = cachedData?.accountId ?: wallet.address
        if (accountId.isBlank()) return
        cacheData(
            accountId = accountId,
            associatedTokens = cachedData?.associatedTokens.orEmpty(),
            tokenTypes = tokenTypes,
            tokenEvmAddresses = tokenEvmAddresses,
            resolvedContractAddresses = resolvedContractAddresses,
        )
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