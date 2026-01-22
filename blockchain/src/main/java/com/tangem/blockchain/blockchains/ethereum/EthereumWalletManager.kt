package com.tangem.blockchain.blockchains.ethereum

import android.util.Log
import com.tangem.blockchain.blockchains.ethereum.eip1559.isSupportEIP1559
import com.tangem.blockchain.blockchains.ethereum.ens.DefaultENSNameProcessor
import com.tangem.blockchain.blockchains.ethereum.gasless.DefaultEthereumGaslessDataProvider
import com.tangem.blockchain.blockchains.ethereum.gasless.EthereumGaslessDataProvider
import com.tangem.blockchain.blockchains.ethereum.gasless.GaslessContractAddressFactory
import com.tangem.blockchain.blockchains.ethereum.network.EthereumFeeHistory
import com.tangem.blockchain.blockchains.ethereum.network.EthereumInfoResponse
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.ApprovalERC20TokenCallData
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumCompiledTxInfo
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionValidator
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.di.DepsContainer
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.common.transaction.TransactionsSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.Result.Failure
import com.tangem.blockchain.extensions.Result.Success
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.nft.DefaultNFTProvider
import com.tangem.blockchain.nft.NFTProvider
import com.tangem.blockchain.pendingtransactions.DefaultPendingTransactionsProvider
import com.tangem.blockchain.pendingtransactions.PendingTransactionStatus
import com.tangem.blockchain.pendingtransactions.PendingTransactionsProvider
import com.tangem.blockchain.transactionhistory.DefaultTransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
import com.tangem.blockchain.yieldsupply.DefaultYieldSupplyProvider
import com.tangem.blockchain.yieldsupply.YieldSupplyProvider
import com.tangem.common.CompletionResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.kethereum.extensions.toHexString
import org.kethereum.keccakshortcut.keccak
import org.komputing.khex.extensions.toHexString
import java.math.BigDecimal
import java.math.BigInteger

@Suppress("LargeClass", "TooManyFunctions")
open class EthereumWalletManager(
    wallet: Wallet,
    val transactionBuilder: EthereumTransactionBuilder,
    protected val networkProvider: EthereumNetworkProvider,
    transactionHistoryProvider: TransactionHistoryProvider = DefaultTransactionHistoryProvider,
    nftProvider: NFTProvider = DefaultNFTProvider,
    private val supportsENS: Boolean,
    yieldSupplyProvider: YieldSupplyProvider = DefaultYieldSupplyProvider,
    private val pendingTransactionsProvider: PendingTransactionsProvider = DefaultPendingTransactionsProvider,
    ethereumGaslessDataProvider: EthereumGaslessDataProvider = DefaultEthereumGaslessDataProvider(
        wallet = wallet,
        networkProvider = networkProvider,
        gaslessContractAddressFactory = GaslessContractAddressFactory(wallet.blockchain),
    ),
) : WalletManager(
    wallet = wallet,
    transactionHistoryProvider = transactionHistoryProvider,
    nftProvider = nftProvider,
    yieldSupplyProvider = yieldSupplyProvider,
),
    SignatureCountValidator,
    TokenFinder,
    EthereumGasLoader,
    TransactionPreparer,
    Approver,
    NameResolver,
    PendingTransactionHandler,
    TransactionValidator by EthereumTransactionValidator,
    EthereumGaslessDataProvider by ethereumGaslessDataProvider {

    // move to constructor later
    protected val feesCalculator = EthereumFeesCalculator()

    private val ensNameProcessor = DefaultENSNameProcessor()

    private var pendingTxCount = -1L

    private var txCount = -1L

    override val currentHost: String
        get() = networkProvider.baseUrl

    override suspend fun updateInternal() {
        when (val result = networkProvider.getInfo(wallet.address, cardTokens)) {
            is Result.Success -> updateWallet(result.data)
            is Result.Failure -> updateError(result.error)
        }
    }

    private suspend fun updateWallet(data: EthereumInfoResponse) {
        wallet.setCoinValue(data.coinBalance)
        // Reset tokens and add again to avoid duplicates
        wallet.removeAllTokens()
        data.tokenBalances.forEach { wallet.setAmount(it) }

        txCount = data.txCount
        pendingTxCount = data.pendingTxCount

        if (DepsContainer.blockchainFeatureToggles.isPendingTransactionsEnabled) {
            val pendingTransactionUpdate = pendingTransactionsProvider.checkPendingTransactions()
            val pendingTransactions = pendingTransactionsProvider.getPendingTransactions(null)

            updatePendingTransactions(
                pendingTransactionUpdate = pendingTransactionUpdate,
                pendingTransactions = pendingTransactions,
                txCount = txCount,
                pendingTxCount = pendingTxCount,
            )
        } else {
            if (txCount == pendingTxCount) {
                wallet.recentTransactions.forEach { it.status = TransactionStatus.Confirmed }
            } else if (!data.recentTransactions.isNullOrEmpty()) {
                updateRecentTransactions(data.recentTransactions)
            } else {
                wallet.addTransactionDummy()
            }
        }

        if (supportsENS) {
            try {
                val ensResult = reverseResolve(wallet.address)
                when (ensResult) {
                    is ReverseResolveAddressResult.Error -> {
                        Log.w(this::class.java.simpleName, "Failed to fetch ENS name: ${ensResult.error}")
                    }
                    ReverseResolveAddressResult.NotSupported -> {
                        Log.w(this::class.java.simpleName, "Ens not supported")
                    }
                    is ReverseResolveAddressResult.Resolved -> wallet.setEnsName(ensResult.name)
                }
            } catch (e: Exception) {
                Log.w(this::class.java.simpleName, "Failed to fetch ENS name: $e")
            }
        }
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) throw error
    }

    internal fun updatePendingTransactions(
        pendingTransactionUpdate: Map<String, PendingTransactionStatus>,
        pendingTransactions: List<String>,
        txCount: Long,
        pendingTxCount: Long,
    ) {
        pendingTransactionUpdate.forEach { (txId, status) ->
            when (status) {
                PendingTransactionStatus.Executed -> {
                    wallet.recentTransactions.find { it.hash == txId }?.status = TransactionStatus.Confirmed
                }
                PendingTransactionStatus.Dropped -> {
                    wallet.recentTransactions.removeAll { it.hash == txId }
                }
                PendingTransactionStatus.Pending -> {
                    // Transaction is still pending, no action needed
                }
            }
        }

        val pendingBlockchainCount = pendingTxCount - txCount

        if (pendingBlockchainCount > pendingTransactions.size) {
            wallet.addTransactionDummy()
        } else if (pendingBlockchainCount <= pendingTransactions.size) {
            wallet.recentTransactions.removeAll { it.hash == null }

            pendingTransactions.forEach { txId ->
                if (wallet.recentTransactions.none { it.hash == txId }) {
                    wallet.addPendingTransactionDummy(txId)
                }
            }
        }
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        validate(transactionData).onFailure {
            return Result.Failure(it as? BlockchainSdkError ?: BlockchainSdkError.FailedToBuildTx)
        }

        val transactionToSend = prepareForSend(transactionData, signer)
            .successOr { return it }

        return when (val sendResult = networkProvider.sendTransaction(transactionToSend.toHexString())) {
            is Failure -> Result.fromTangemSdkError(sendResult.error)
            is Success<*> -> {
                val txHash = transactionToSend.keccak().toHexString()
                wallet.addOutgoingTransaction(transactionData = transactionData, txHash = txHash)

                val contractAddress = (transactionData as? TransactionData.Uncompiled)?.contractAddress
                pendingTransactionsProvider.addPendingTransaction(txHash, networkProvider, contractAddress)

                Success(TransactionSendResult(txHash))
            }
        }
    }

    override suspend fun broadcastTransaction(signedTransaction: String): Result<TransactionSendResult> {
        return when (val sendResult = networkProvider.sendTransaction(signedTransaction)) {
            is Failure -> Result.fromTangemSdkError(sendResult.error)
            is Success<String> -> {
                val txHash = sendResult.data
                val transactionData = TransactionData.Compiled(
                    value = TransactionData.Compiled.Data.RawString(signedTransaction),
                )
                wallet.addOutgoingTransaction(transactionData = transactionData, txHash = txHash)
                Success(TransactionSendResult(txHash))
            }
        }
    }

    override suspend fun sendMultiple(
        transactionDataList: List<TransactionData>,
        signer: TransactionSigner,
        sendMode: TransactionSender.MultipleTransactionSendMode,
    ): Result<TransactionsSendResult> {
        transactionDataList.forEach { transactionData ->
            validate(transactionData).onFailure {
                return Result.Failure(it as? BlockchainSdkError ?: BlockchainSdkError.FailedToBuildTx)
            }
        }
        if (transactionDataList.size == 1) {
            return sendSingleTransaction(transactionDataList, signer)
        }

        val blockchainNonce = networkProvider
            .getPendingTxCount(wallet.address)
            .successOr { return Result.Failure(BlockchainSdkError.FailedToBuildTx) }
            .toBigInteger()

        val updatedData = transactionDataList.mapIndexed { index, data ->
            val uncompiledTransaction = data.requireUncompiled()
            uncompiledTransaction.copy(
                extras = (uncompiledTransaction.extras as? EthereumTransactionExtras)
                    ?.copy(nonce = blockchainNonce + index.toBigInteger())
                    ?: EthereumTransactionExtras(nonce = blockchainNonce + index.toBigInteger()),
            )
        }

        val signedTxs = when (val signResult = signMultiple(updatedData, signer)) {
            is Result.Failure -> return Result.Failure(signResult.error)
            is Result.Success -> signResult.data
        }

        val sendResults = updatedData.mapIndexed { index, data ->
            val transactionToSend = try {
                transactionBuilder.buildForSend(
                    transaction = data,
                    signature = signedTxs.keys.elementAt(index),
                    compiledTransaction = signedTxs.values.elementAt(index),
                )
            } catch (e: BlockchainSdkError.FailedToBuildTx) {
                return Result.Failure(e)
            }

            when (val sendResult = networkProvider.sendTransaction(transactionToSend.toHexString())) {
                is Result.Failure -> Failure(sendResult.error)
                is Result.Success<*> -> {
                    val txHash = transactionToSend.keccak().toHexString()
                    wallet.addOutgoingTransaction(transactionData = data, txHash = txHash)

                    val contractAddress = data.contractAddress
                    pendingTransactionsProvider.addPendingTransaction(txHash, networkProvider, contractAddress)

                    Success(TransactionSendResult(txHash))
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

    protected open suspend fun sign(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<Pair<ByteArray, EthereumCompiledTxInfo>> {
        val transactionToSign = transactionBuilder.buildForSign(transaction = transactionData)

        return when (val signResponse = signer.sign(transactionToSign.hash, wallet.publicKey)) {
            is CompletionResult.Success -> Result.Success(signResponse.data to transactionToSign)
            is CompletionResult.Failure -> Result.fromTangemSdkError(signResponse.error)
        }
    }

    protected open suspend fun signMultiple(
        transactionDataList: List<TransactionData>,
        signer: TransactionSigner,
    ): Result<Map<ByteArray, EthereumCompiledTxInfo>> {
        val transactionToSignHashes = transactionDataList.map {
            transactionBuilder.buildForSign(transaction = it)
        }

        return when (val signResponse = signer.sign(transactionToSignHashes.map { it.hash }, wallet.publicKey)) {
            is CompletionResult.Success -> {
                val resultMap = mutableMapOf<ByteArray, EthereumCompiledTxInfo>()
                signResponse.data.forEachIndexed { index, signature ->
                    resultMap[signature] = transactionToSignHashes[index]
                }
                Result.Success(resultMap)
            }
            is CompletionResult.Failure -> Result.fromTangemSdkError(signResponse.error)
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        return getFeeInternal(amount, destination, null)
    }

    override suspend fun getFee(
        amount: Amount,
        destination: String,
        callData: SmartContractCallData?,
    ): Result<TransactionFee> {
        return getFeeInternal(amount, destination, callData)
    }

    override suspend fun getFee(transactionData: TransactionData): Result<TransactionFee> {
        val uncompiled = transactionData.requireUncompiled()
        val extra = uncompiled.extras as? EthereumTransactionExtras
        return getFeeInternal(
            amount = uncompiled.amount,
            destination = uncompiled.destinationAddress,
            callData = extra?.callData,
        )
    }

    protected open suspend fun getFeeInternal(
        amount: Amount,
        destination: String,
        callData: SmartContractCallData?,
    ): Result<TransactionFee> {
        return if (wallet.blockchain.isSupportEIP1559) {
            getEIP1559Fee(amount, destination, callData)
        } else {
            getLegacyFee(amount, destination, callData)
        }
    }

    protected fun getAmountParams(): Amount {
        return requireNotNull(wallet.amounts[AmountType.Coin]) { "Amount must not be null" }
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

    override suspend fun findTokens(): Result<List<Token>> {
        return when (val result = networkProvider.findErc20Tokens(wallet.address)) {
            is Result.Failure -> Result.Failure(result.error)
            is Result.Success -> {
                val tokens: List<Token> = result.data.map { blockchairToken ->
                    val token = blockchairToken.toToken()
                    if (!cardTokens.contains(token)) {
                        cardTokens.add(token)
                    }
                    val balance =
                        blockchairToken.balance.toBigDecimalOrNull()?.movePointLeft(blockchairToken.decimals)
                            ?: BigDecimal.ZERO
                    wallet.addTokenValue(balance, token)
                    token
                }
                return Result.Success(tokens)
            }
        }
    }

    override suspend fun getGasPrice(): Result<BigInteger> {
        return networkProvider.getGasPrice()
    }

    override suspend fun getGasHistory(): Result<EthereumFeeHistory> {
        return networkProvider.getFeeHistory()
    }

    override suspend fun getGasLimit(amount: Amount, destination: String): Result<BigInteger> {
        return getGasLimitInternal(amount, destination, null)
    }

    override suspend fun getGasLimit(
        amount: Amount,
        destination: String,
        callData: SmartContractCallData,
    ): Result<BigInteger> {
        return getGasLimitInternal(amount, destination, callData)
    }

    override suspend fun getAllowance(spenderAddress: String, token: Token): kotlin.Result<BigDecimal> {
        return networkProvider.getAllowance(wallet.address, token, spenderAddress)
    }

    override fun getApproveData(spenderAddress: String, value: Amount?) = ApprovalERC20TokenCallData(
        spenderAddress = spenderAddress,
        amount = value,
    ).dataHex

    override suspend fun prepareForSend(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<ByteArray> {
        val transactionData = when (transactionData) {
            is TransactionData.Uncompiled -> {
                val blockchainNonce = networkProvider
                    .getPendingTxCount(wallet.address)
                    .successOr { return Result.Failure(BlockchainSdkError.FailedToBuildTx) }
                    .toBigInteger()
                val newExtras = when (val extras = transactionData.extras) {
                    is EthereumTransactionExtras -> {
                        val customNonce = extras.nonce
                        val actualNonce = customNonce?.let {
                            if (customNonce > blockchainNonce) {
                                blockchainNonce
                            } else {
                                customNonce
                            }
                        } ?: blockchainNonce
                        extras.copy(nonce = actualNonce)
                    }
                    else -> EthereumTransactionExtras(nonce = blockchainNonce)
                }
                transactionData.copy(extras = newExtras)
            }
            is TransactionData.Compiled -> transactionData
        }
        return when (val signResponse = sign(transactionData, signer)) {
            is Result.Success -> {
                val transactionToSend = try {
                    transactionBuilder.buildForSend(
                        transaction = transactionData,
                        signature = signResponse.data.first,
                        compiledTransaction = signResponse.data.second,
                    )
                } catch (e: BlockchainSdkError.FailedToBuildTx) {
                    return Result.Failure(e)
                }

                return Result.Success(transactionToSend)
            }
            is Result.Failure -> Result.fromTangemSdkError(signResponse.error)
        }
    }

    override suspend fun prepareAndSign(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<ByteArray> {
        TODO("[REDACTED_JIRA]")
    }

    override suspend fun prepareAndSignMultiple(
        transactionDataList: List<TransactionData>,
        signer: TransactionSigner,
    ): Result<List<ByteArray>> {
        TODO("[REDACTED_JIRA]")
    }

    override suspend fun resolve(name: String): ResolveAddressResult {
        if (supportsENS) {
            val namehash = ensNameProcessor.getNamehash(name).successOr { return ResolveAddressResult.Error(it.error) }
            val encodedName = ensNameProcessor.encode(name).successOr { return ResolveAddressResult.Error(it.error) }

            return networkProvider.resolveName(namehash, encodedName)
        } else {
            return ResolveAddressResult.NotSupported
        }
    }

    override suspend fun reverseResolve(address: String): ReverseResolveAddressResult {
        return if (supportsENS) {
            networkProvider.resolveAddress(address)
        } else {
            ReverseResolveAddressResult.NotSupported
        }
    }

    private suspend fun getGasLimitInternal(
        amount: Amount,
        destination: String,
        callData: SmartContractCallData? = null,
    ): Result<BigInteger> {
        val from = wallet.address
        var to = destination
        var value: String? = null
        val data: String? = callData?.dataHex

        when (amount.type) {
            is AmountType.Coin -> {
                value = amount.value?.movePointRight(amount.decimals)?.toBigInteger()?.toHexString()
            }

            is AmountType.Token -> {
                to = amount.type.token.contractAddress
            }

            else -> {
                /* no-op */
            }
        }

        return networkProvider.getGasLimit(to, from, value, data)
    }

    private suspend fun getEIP1559Fee(
        amount: Amount,
        destination: String,
        callData: SmartContractCallData?,
    ): Result<TransactionFee.Choosable> {
        return try {
            coroutineScope {
                val gasLimitResponsesDeferred = async {
                    if (callData != null) {
                        getGasLimit(amount, destination, callData)
                    } else {
                        getGasLimit(amount, destination)
                    }
                }

                val feeHistoryResponseDeferred = async { networkProvider.getFeeHistory() }

                val gLimit = gasLimitResponsesDeferred.await().successOr {
                    return@coroutineScope Result.Failure(it.error)
                }

                val feeHistory = feeHistoryResponseDeferred.await().successOr {
                    return@coroutineScope Result.Failure(it.error)
                }

                val fees = feesCalculator.calculateEip1559Fees(
                    amountParams = getAmountParams(),
                    gasLimit = gLimit,
                    feeHistory = feeHistory,
                )

                Result.Success(fees)
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    private suspend fun getLegacyFee(
        amount: Amount,
        destination: String,
        callData: SmartContractCallData?,
    ): Result<TransactionFee.Choosable> {
        return try {
            coroutineScope {
                val gasLimitResponsesDeferred = async {
                    if (callData != null) {
                        getGasLimit(amount, destination, callData)
                    } else {
                        getGasLimit(amount, destination)
                    }
                }
                val gasPriceResponsesDeferred = async { getGasPrice() }

                val gLimit = gasLimitResponsesDeferred.await().successOr {
                    return@coroutineScope Result.Failure(it.error)
                }
                val gPrice = gasPriceResponsesDeferred.await().successOr {
                    return@coroutineScope Result.Failure(it.error)
                }

                val fees = feesCalculator.calculateFees(
                    amountParams = getAmountParams(),
                    gasLimit = gLimit,
                    gasPrice = gPrice,
                )

                Result.Success(fees)
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    override suspend fun getPendingTransactions(contractAddress: String?): List<String> {
        return pendingTransactionsProvider.getPendingTransactions(contractAddress)
    }

    override suspend fun addPendingGaslessTransaction(
        transactionData: TransactionData,
        txHash: String,
        contractAddress: String?,
    ) {
        wallet.addOutgoingTransaction(transactionData, txHash)
        pendingTransactionsProvider.addPendingGaslessTransaction(txHash, contractAddress)
    }
}