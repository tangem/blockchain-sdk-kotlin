package com.tangem.blockchain.blockchains.ethereum

import android.util.Log
import com.tangem.blockchain.blockchains.ethereum.network.EthereumInfoResponse
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainError
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.SignatureCountValidator
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TokenFinder
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionHistoryProvider
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.kethereum.extensions.toHexString
import org.kethereum.keccakshortcut.keccak
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import java.util.Calendar

open class EthereumWalletManager(
    wallet: Wallet,
    val transactionBuilder: EthereumTransactionBuilder,
    protected val networkProvider: EthereumNetworkProvider,
    presetTokens: MutableSet<Token>,
) : WalletManager(wallet, presetTokens),
    TransactionSender,
    TransactionHistoryProvider,
    SignatureCountValidator,
    TokenFinder,
    EthereumGasLoader {

    var pendingTxCount = -1L
        private set
    var txCount = -1L
        private set
    var gasLimit: BigInteger? = null
        private set
    var gasLimitToApprove: BigInteger? = null
        private set
    var gasLimitToSetSpendLimit: BigInteger? = null
        private set
    var gasLimitToInitOTP: BigInteger? = null
        private set
    var gasLimitToSetWallet: BigInteger? = null
        private set
    var gasLimitToTransferFrom: BigInteger? = null
        private set
    var gasPrice: BigInteger? = null
        private set

    override val currentHost: String
        get() = networkProvider.host

    override suspend fun update() {
        when (val result = networkProvider.getInfo(wallet.address, cardTokens)) {
            is Result.Success -> updateWallet(result.data)
            is Result.Failure -> updateError(result.error)
        }
    }

    private fun updateWallet(data: EthereumInfoResponse) {
        wallet.setCoinValue(data.coinBalance)
        data.tokenBalances.forEach { wallet.addTokenValue(it.value, it.key) }

        txCount = data.txCount
        pendingTxCount = data.pendingTxCount

        if (txCount == pendingTxCount) {
            wallet.recentTransactions.forEach { it.status = TransactionStatus.Confirmed }
        } else if (!data.recentTransactions.isNullOrEmpty()) {
            updateRecentTransactions(data.recentTransactions)
        } else {
            wallet.addTransactionDummy()
        }
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) throw error
    }

    suspend fun sendRaw(transactionToSign: CompiledEthereumTransaction, signature: ByteArray): SimpleResult {
        val transactionToSend = transactionBuilder.buildToSend(signature, transactionToSign)
        return networkProvider.sendTransaction("0x" + transactionToSend.toHexString())
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): SimpleResult {
        return when (val signResponse = sign(transactionData, signer)) {
            is Result.Success -> {
                val transactionToSend = transactionBuilder
                    .buildToSend(
                        signature = signResponse.data.first,
                        transactionToSign = signResponse.data.second
                    )
                val sendResult = networkProvider
                    .sendTransaction("0x" + transactionToSend.toHexString())

                if (sendResult is SimpleResult.Success) {
                    transactionData.hash = transactionToSend.keccak().toHexString()
                    wallet.addOutgoingTransaction(transactionData)
                }
                sendResult
            }
            is Result.Failure -> SimpleResult.fromTangemSdkError(signResponse.error)
        }
    }

    open suspend fun sign(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<Pair<ByteArray, CompiledEthereumTransaction>> {
        val transactionToSign = transactionBuilder.buildToSign(
            transactionData,
            txCount.toBigInteger(),
            gasLimit
        ) ?: return Result.Failure(BlockchainSdkError.CustomError("Not enough data"))

        return when (val signResponse = signer.sign(transactionToSign.hash, wallet.publicKey)) {
            is CompletionResult.Success -> Result.Success(signResponse.data to transactionToSign)
            is CompletionResult.Failure -> Result.fromTangemSdkError(signResponse.error)
        }
    }

    suspend fun signAndSend(
        transactionToSign: CompiledEthereumTransaction,
        signer: TransactionSigner,
    ): SimpleResult {
        return when (val signerResponse = signer.sign(transactionToSign.hash, wallet.publicKey)) {
            is CompletionResult.Success -> {
                val transactionToSend = transactionBuilder.buildToSend(signerResponse.data, transactionToSign)
                val sendResult = networkProvider.sendTransaction("0x" + transactionToSend.toHexString())
                sendResult
            }
            is CompletionResult.Failure -> SimpleResult.fromTangemSdkError(signerResponse.error)
        }
    }

    suspend fun approve(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): SimpleResult {
        val transactionToSign = transactionBuilder.buildApproveToSign(
            transactionData,
            txCount.toBigInteger(),
            gasLimitToApprove
        ) ?: return SimpleResult.Failure(BlockchainSdkError.CustomError("Not enough data"))

        return signAndSend(transactionToSign, signer)
    }

    suspend fun getAllowance(spender: String, token: Token): Result<Amount> {
        return networkProvider.getAllowance(spender, token, wallet.address)
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        return try {
            coroutineScope {
                val gasLimitResponsesDeferred =
                    async { getGasLimit(amount, destination) }
                val gasPriceResponsesDeferred = async { getGasPrice() }

                val gLimit = when (val gasLimitResult = gasLimitResponsesDeferred.await()) {
                    is Result.Failure -> return@coroutineScope Result.Failure(gasLimitResult.error)
                    is Result.Success -> gasLimitResult.data
                }
                val gPrice = when (val gasPriceResult = gasPriceResponsesDeferred.await()) {
                    is Result.Failure -> return@coroutineScope Result.Failure(gasPriceResult.error)
                    is Result.Success -> gasPriceResult.data
                }

                gasLimit = gLimit
                gasPrice = gPrice
                val fees = calculateFees(gLimit, gPrice).map { value ->
                    Amount(wallet.amounts[AmountType.Coin]!!, value)
                }
                Result.Success(fees)
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    suspend fun getFeeToApprove(amount: Amount, spender: String): Result<List<Amount>> {
        return try {
            coroutineScope {
                val gasLimitResponsesDeferred =
                    async { getGasLimitToApprove(amount, spender) }
                val gasPriceResponsesDeferred = async { getGasPrice() }

                val gLimit = when (val gasLimitResult = gasLimitResponsesDeferred.await()) {
                    is Result.Failure -> return@coroutineScope Result.Failure(gasLimitResult.error)
                    is Result.Success -> gasLimitResult.data
                }
                val gPrice = when (val gasPriceResult = gasPriceResponsesDeferred.await()) {
                    is Result.Failure -> return@coroutineScope Result.Failure(gasPriceResult.error)
                    is Result.Success -> gasPriceResult.data
                }
                gasLimitToApprove = gLimit
                gasPrice = gPrice

                val fees = calculateFees(gLimit, gPrice)
                    .map { value -> Amount(wallet.amounts[AmountType.Coin]!!, value) }
                Result.Success(fees)
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    suspend fun getFeeToInitOTP(
        processorContractAddress: String,
        otp: ByteArray,
        otpCounter: Int,
    ): Result<List<Amount>> {
        return try {
            coroutineScope {
                val gasLimitResponsesDeferred =
                    async { getGasLimitToInitOTP(processorContractAddress, wallet.address, otp, otpCounter) }
                val gasPriceResponsesDeferred = async { getGasPrice() }

                val gLimit = when (val gasLimitResult = gasLimitResponsesDeferred.await()) {
                    is Result.Failure -> return@coroutineScope Result.Failure(gasLimitResult.error)
                    is Result.Success -> gasLimitResult.data
                }
                val gPrice = when (val gasPriceResult = gasPriceResponsesDeferred.await()) {
                    is Result.Failure -> return@coroutineScope Result.Failure(gasPriceResult.error)
                    is Result.Success -> gasPriceResult.data
                }
                gasLimitToInitOTP = gLimit
                gasPrice = gPrice

                val fees = calculateFees(gLimit, gPrice)
                    .map { value -> Amount(wallet.amounts[AmountType.Coin]!!, value) }
                Result.Success(fees)
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    suspend fun initOTP(
        processorContractAddress: String,
        cardAddress: String,
        otp: ByteArray, otpCounter: Int,
        transactionFee: Amount?,
        signer: TransactionSigner,
    ): SimpleResult {
        val transactionToSign = transactionBuilder.buildInitOTPToSign(
            processorContractAddress,
            cardAddress,
            otp,
            otpCounter,
            transactionFee,
            gasLimitToInitOTP,
            txCount.toBigInteger(),
        ) ?: return SimpleResult.Failure(BlockchainSdkError.CustomError("Not enough data"))

        return signAndSend(transactionToSign, signer)
    }

    suspend fun setWallet(
        processorContractAddress: String,
        cardAddress: String,
        transactionFee: Amount?,
        signer: TransactionSigner,
    ): SimpleResult {
        val transactionToSign = transactionBuilder.buildSetWalletToSign(
            processorContractAddress,
            cardAddress,
            transactionFee,
            gasLimitToSetWallet,
            txCount.toBigInteger(),
        ) ?: return SimpleResult.Failure(BlockchainSdkError.CustomError("Not enough data"))

        return signAndSend(transactionToSign, signer)
    }

    suspend fun getFeeToSetWallet(processorContractAddress: String): Result<List<Amount>> {
        return try {
            coroutineScope {
                val gasLimitResponsesDeferred =
                    async { getGasLimitToSetWallet(processorContractAddress, wallet.address) }
                val gasPriceResponsesDeferred = async { getGasPrice() }

                val gLimit = when (val gasLimitResult = gasLimitResponsesDeferred.await()) {
                    is Result.Failure -> return@coroutineScope Result.Failure(gasLimitResult.error)
                    is Result.Success -> gasLimitResult.data
                }
                val gPrice = when (val gasPriceResult = gasPriceResponsesDeferred.await()) {
                    is Result.Failure -> return@coroutineScope Result.Failure(gasPriceResult.error)
                    is Result.Success -> gasPriceResult.data
                }
                gasLimitToSetWallet = gLimit
                gasPrice = gPrice

                val fees = calculateFees(gLimit, gPrice)
                    .map { value -> Amount(wallet.amounts[AmountType.Coin]!!, value) }
                Result.Success(fees)
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    suspend fun getFeeToSetSpendLimit(processorContractAddress: String, amount: Amount): Result<List<Amount>> {
        return try {
            coroutineScope {
                val gasLimitResponsesDeferred =
                    async { getGasLimitToSetSpendLimit(processorContractAddress, wallet.address, amount) }
                val gasPriceResponsesDeferred = async { getGasPrice() }

                val gLimit = when (val gasLimitResult = gasLimitResponsesDeferred.await()) {
                    is Result.Failure -> return@coroutineScope Result.Failure(gasLimitResult.error)
                    is Result.Success -> gasLimitResult.data
                }
                val gPrice = when (val gasPriceResult = gasPriceResponsesDeferred.await()) {
                    is Result.Failure -> return@coroutineScope Result.Failure(gasPriceResult.error)
                    is Result.Success -> gasPriceResult.data
                }
                gasLimitToSetSpendLimit = gLimit
                gasPrice = gPrice

                val fees = calculateFees(gLimit, gPrice).map { value ->
                    Amount(wallet.amounts[AmountType.Coin]!!, value)
                }
                Result.Success(fees)
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    suspend fun getFeeToTransferFrom(amount: Amount, source: String): Result<List<Amount>> {
        return try {
            coroutineScope {
                val gasLimitResponsesDeferred =
                    async { getGasLimitToTransferFrom(amount, source, wallet.address) }
                val gasPriceResponsesDeferred = async { getGasPrice() }

                val gLimit = when (val gasLimitResult = gasLimitResponsesDeferred.await()) {
                    is Result.Failure -> return@coroutineScope Result.Failure(gasLimitResult.error)
                    is Result.Success -> gasLimitResult.data
                }
                val gPrice = when (val gasPriceResult = gasPriceResponsesDeferred.await()) {
                    is Result.Failure -> return@coroutineScope Result.Failure(gasPriceResult.error)
                    is Result.Success -> gasPriceResult.data
                }

                gasLimitToTransferFrom = gLimit
                gasPrice = gPrice
                val fees = calculateFees(gLimit, gPrice)
                    .map { value -> Amount(wallet.amounts[AmountType.Coin]!!, value) }
                Result.Success(fees)
            }
        } catch (exception: Exception) {
            Result.Failure(exception.toBlockchainSdkError())
        }
    }

    suspend fun setSpendLimit(
        processorContractAddress: String,
        cardAddress: String,
        amount: Amount,
        transactionFee: Amount?,
        signer: TransactionSigner,
    ): SimpleResult {
        val transactionToSign = transactionBuilder.buildSetSpendLimitToSign(
            processorContractAddress,
            cardAddress,
            amount,
            transactionFee,
            gasLimitToSetSpendLimit,
            txCount.toBigInteger(),
        ) ?: return SimpleResult.Failure(BlockchainSdkError.CustomError("Not enough data"))

        return signAndSend(transactionToSign, signer)
    }

    suspend fun transferFrom(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val transactionToSign = transactionBuilder.buildTransferFromToSign(
            transactionData,
            txCount.toBigInteger(),
            gasLimitToTransferFrom
        ) ?: return SimpleResult.Failure(BlockchainSdkError.CustomError("Not enough data"))

        return signAndSend(transactionToSign, signer)
    }

    open fun createTransferFromTransaction(amount: Amount, fee: Amount, source: String): TransactionData {
        return TransactionData(
            amount = amount,
            fee = fee,
            sourceAddress = source,
            destinationAddress = wallet.address,
            status = TransactionStatus.Unconfirmed,
            date = Calendar.getInstance(),
            hash = null,
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

    override suspend fun findTokens(): Result<List<Token>> {
        return when (val result = networkProvider.findErc20Tokens(wallet.address)) {
            is Result.Failure -> Result.Failure(result.error)
            is Result.Success -> {
                val tokens: List<Token> = result.data.map { blockchairToken ->
                    val token = blockchairToken.toToken()
                    if (!cardTokens.contains(token)) {
                        cardTokens.add(token)
                    }
                    val balance = blockchairToken.balance.toBigDecimalOrNull()
                        ?.movePointLeft(blockchairToken.decimals)
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

    override suspend fun getGasLimit(amount: Amount, destination: String): Result<BigInteger> {
        var to = destination
        val from = wallet.address
        var value: String? = null
        var data: String? = null

        when (amount.type) {
            is AmountType.Coin -> {
                value = amount.value?.movePointRight(amount.decimals)?.toBigInteger()?.toHexString()
            }
            is AmountType.Token -> {
                to = amount.type.token.contractAddress
                data = "0x" + EthereumUtils.createErc20TransferData(destination, amount).toHexString()
            }
        }

        return networkProvider.getGasLimit(to, from, value, data)
    }

    suspend fun getGasLimitToApprove(amount: Amount, spender: String): Result<BigInteger> {
        return if (amount.type !is AmountType.Token) {
            Result.Failure(BlockchainSdkError.CustomError("Only token can be approved!!!"))
        } else {
            val to = amount.type.token.contractAddress
            val from = wallet.address
            val data = "0x" + EthereumUtils.createErc20ApproveData(spender, amount).toHexString()

            networkProvider.getGasLimit(to, from, null, data)
        }
    }

    suspend fun getGasLimitToSetSpendLimit(
        processorContractAddress: String,
        cardAddress: String,
        amount: Amount,
    ): Result<BigInteger> {
        val from = wallet.address
        val data = "0x" + EthereumUtils.createSetSpendLimitData(cardAddress, amount).toHexString()

        return networkProvider.getGasLimit(processorContractAddress, from, null, data)
    }

    suspend fun getGasLimitToInitOTP(
        processorContractAddress: String,
        cardAddress: String,
        otp: ByteArray,
        otpCounter: Int,
    ): Result<BigInteger> {
        val from = wallet.address
        val data = "0x" + EthereumUtils.createInitOTPData(otp, otpCounter).toHexString()

        return networkProvider.getGasLimit(processorContractAddress, from, null, data)
    }

    suspend fun getGasLimitToSetWallet(processorContractAddress: String, address: String): Result<BigInteger> {
        val from = wallet.address
        val data = "0x" + EthereumUtils.createSetWalletData(address).toHexString()

        return networkProvider.getGasLimit(processorContractAddress, from, null, data)
    }

    suspend fun getGasLimitToTransferFrom(amount: Amount, source: String, destination: String): Result<BigInteger> {
        return if (amount.type !is AmountType.Token) {
            Result.Failure(BlockchainSdkError.CustomError("Only token can be transferred!!!"))
        } else {
            val to = amount.type.token.contractAddress
            val from = wallet.address
            val data = "0x" + EthereumUtils.createErc20TransferFromData(source, destination, amount).toHexString()

            networkProvider.getGasLimit(to, from, null, data)
        }
    }

    protected open fun calculateFees(gasLimit: BigInteger, gasPrice: BigInteger): List<BigDecimal> {
        val minFee = gasPrice * gasLimit
        //By dividing by ten before last multiplication here we can lose some digits
        val normalFee = gasPrice * BigInteger.valueOf(12) / BigInteger.TEN * gasLimit
        val priorityFee = gasPrice * BigInteger.valueOf(15) / BigInteger.TEN * gasLimit

        val decimals = Blockchain.Ethereum.decimals()
        return listOf(minFee, normalFee, priorityFee)
            .map {
                it.toBigDecimal(
                    scale = decimals,
                    mathContext = MathContext(decimals, RoundingMode.HALF_EVEN)
                )
            }
    }

    override suspend fun getTransactionHistory(
        address: String,
        blockchain: Blockchain,
        tokens: Set<Token>,
    ): Result<List<TransactionData>> = networkProvider.getTransactionHistory(address, blockchain, tokens)
}
