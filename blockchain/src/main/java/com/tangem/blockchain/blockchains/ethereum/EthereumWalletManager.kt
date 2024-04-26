package com.tangem.blockchain.blockchains.ethereum

import android.util.Log
import com.tangem.blockchain.blockchains.ethereum.network.EthereumInfoResponse
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.txhistory.DefaultTransactionHistoryProvider
import com.tangem.blockchain.common.txhistory.TransactionHistoryProvider
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.successOr
import com.tangem.common.CompletionResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.kethereum.extensions.toHexString
import org.kethereum.keccakshortcut.keccak
import org.komputing.khex.extensions.toHexString
import java.math.BigDecimal
import java.math.BigInteger

open class EthereumWalletManager(
    wallet: Wallet,
    val transactionBuilder: EthereumTransactionBuilder,
    protected val networkProvider: EthereumNetworkProvider,
    transactionHistoryProvider: TransactionHistoryProvider = DefaultTransactionHistoryProvider,
) : WalletManager(wallet, transactionHistoryProvider = transactionHistoryProvider),
    SignatureCountValidator,
    TokenFinder,
    EthereumGasLoader,
    Approver {

    // move to constructor later
    protected val feesCalculator = EthereumFeesCalculator()

    var pendingTxCount = -1L
        private set
    var txCount = -1L
        private set

    override val currentHost: String
        get() = networkProvider.baseUrl

    override suspend fun updateInternal() {
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

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        return when (val signResponse = sign(transactionData, signer)) {
            is Result.Success -> {
                val transactionToSend = transactionBuilder
                    .buildToSend(
                        signature = signResponse.data.first,
                        transactionToSign = signResponse.data.second,
                    )
                val sendResult = networkProvider
                    .sendTransaction(transactionToSend.toHexString())

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
        ) ?: return Result.Failure(BlockchainSdkError.CustomError("Not enough data"))

        return when (val signResponse = signer.sign(transactionToSign.hash, wallet.publicKey)) {
            is CompletionResult.Success -> Result.Success(signResponse.data to transactionToSign)
            is CompletionResult.Failure -> Result.fromTangemSdkError(signResponse.error)
        }
    }

    suspend fun signAndSend(transactionToSign: CompiledEthereumTransaction, signer: TransactionSigner): SimpleResult {
        return when (val signerResponse = signer.sign(transactionToSign.hash, wallet.publicKey)) {
            is CompletionResult.Success -> {
                val transactionToSend = transactionBuilder.buildToSend(signerResponse.data, transactionToSign)
                val sendResult = networkProvider.sendTransaction(transactionToSend.toHexString())
                sendResult
            }

            is CompletionResult.Failure -> SimpleResult.fromTangemSdkError(signerResponse.error)
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        return getFeeInternal(amount, destination, null)
    }

    open suspend fun getFee(amount: Amount, destination: String, data: String): Result<TransactionFee> {
        return getFeeInternal(amount, destination, data)
    }

    protected open suspend fun getFeeInternal(
        amount: Amount,
        destination: String,
        data: String? = null,
    ): Result<TransactionFee> {
        return try {
            coroutineScope {
                val gasLimitResponsesDeferred = async {
                    if (data != null) {
                        getGasLimit(amount, destination, data)
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
        return getGasLimitInternal(amount, destination, null)
    }

    override suspend fun getGasLimit(amount: Amount, destination: String, data: String): Result<BigInteger> {
        return getGasLimitInternal(amount, destination, data)
    }

    override suspend fun getAllowance(spenderAddress: String, token: Token): Result<BigDecimal> {
        return networkProvider.getAllowance(wallet.address, token, spenderAddress)
    }

    override fun getApproveData(spenderAddress: String, value: Amount?) =
        EthereumUtils.createErc20ApproveDataHex(spenderAddress, value)

    private suspend fun getGasLimitInternal(
        amount: Amount,
        destination: String,
        data: String? = null,
    ): Result<BigInteger> {
        if (!calculateIteratively()) {
            return sendGasLimitRequest(amount, destination, data)
        }

        var delta = MANTLE_FEE_GAP_INITIAL_VALUE
        var newAmount = amount

        for (i in 1..MANTLE_FEE_CALCULATION_STEPS_COUNT) {
            when (val result = sendGasLimitRequest(newAmount, destination, data)) {
                is Result.Success -> {
                    return result
                }
                is Result.Failure -> {
                    if (i == MANTLE_FEE_CALCULATION_STEPS_COUNT ||
                        result.error.cause !is BlockchainSdkError.Ethereum.InsufficientFunds
                    ) {
                        return result
                    }
                }
            }

            newAmount = amount.copy(value = amount.value?.minus(delta))
            delta *= DELTA_INCREASING_STEP
        }

        return Result.Failure(BlockchainSdkError.FailedToLoadFee)
    }

    private suspend fun sendGasLimitRequest(
        amount: Amount,
        destination: String,
        data: String? = null,
    ): Result<BigInteger> {
        val from = wallet.address
        var to = destination
        var value: String? = null
        var finalData = data

        when (amount.type) {
            is AmountType.Coin -> {
                value = amount.value?.movePointRight(amount.decimals)?.toBigInteger()?.toHexString()
            }

            is AmountType.Token -> {
                if (finalData == null) {
                    to = amount.type.token.contractAddress
                    finalData = EthereumUtils.createErc20TransferData(destination, amount).toHexString()
                }
            }

            else -> {
                /*no-op*/
            }
        }

        return networkProvider.getGasLimit(to, from, value, finalData)
    }

    private fun calculateIteratively(): Boolean {
        return wallet.blockchain == Blockchain.Mantle || wallet.blockchain == Blockchain.MantleTestnet
    }

    companion object {
        val MANTLE_FEE_GAP_INITIAL_VALUE = BigDecimal("1E-9")
        val DELTA_INCREASING_STEP = BigDecimal(10)
        const val MANTLE_FEE_CALCULATION_STEPS_COUNT = 5
    }
}