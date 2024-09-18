package com.tangem.blockchain.blockchains.ethereum

import android.util.Log
import com.tangem.blockchain.blockchains.ethereum.eip1559.isSupportEIP1559
import com.tangem.blockchain.blockchains.ethereum.network.EthereumInfoResponse
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumCompiledTxInfo
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionBuilder
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.di.DepsContainer
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.transactionhistory.DefaultTransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
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

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        val nonce = networkProvider.getPendingTxCount(wallet.address)
            .successOr { return Result.Failure(BlockchainSdkError.FailedToBuildTx) }

        val transactionData = when (transactionData) {
            is TransactionData.Uncompiled -> transactionData.copy(
                extras = when (val extras = transactionData.extras) {
                    is EthereumTransactionExtras -> extras.copy(nonce = nonce.toBigInteger())
                    else -> EthereumTransactionExtras(nonce = nonce.toBigInteger())
                },
            )
            is TransactionData.Compiled -> transactionData
        }

        return when (val signResponse = sign(transactionData, signer)) {
            is Result.Success -> {
                val transactionToSend = transactionBuilder.buildForSend(
                    transaction = transactionData,
                    signature = signResponse.data.first,
                    compiledTransaction = signResponse.data.second,
                )

                when (val sendResult = networkProvider.sendTransaction(transactionToSend.toHexString())) {
                    is SimpleResult.Success -> {
                        val hash = transactionToSend.keccak().toHexString()
                        transactionData.hash = hash
                        wallet.addOutgoingTransaction(transactionData)
                        Result.Success(TransactionSendResult(hash))
                    }
                    is SimpleResult.Failure -> Result.fromTangemSdkError(sendResult.error)
                }
            }
            is Result.Failure -> Result.fromTangemSdkError(signResponse.error)
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
        val isEthereumEIP1559Enabled = DepsContainer.blockchainFeatureToggles.isEthereumEIP1559Enabled
        return if (isEthereumEIP1559Enabled && wallet.blockchain.isSupportEIP1559) {
            getEIP1559Fee(amount, destination, data)
        } else {
            getLegacyFee(amount, destination, data)
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

    override suspend fun getAllowance(spenderAddress: String, token: Token): kotlin.Result<BigDecimal> {
        return networkProvider.getAllowance(wallet.address, token, spenderAddress)
    }

    override fun getApproveData(spenderAddress: String, value: Amount?) =
        EthereumUtils.createErc20ApproveDataHex(spenderAddress, value)

    private suspend fun getGasLimitInternal(
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

    private suspend fun getEIP1559Fee(
        amount: Amount,
        destination: String,
        data: String?,
    ): Result<TransactionFee.Choosable> {
        return try {
            coroutineScope {
                val gasLimitResponsesDeferred = async {
                    if (data != null) {
                        getGasLimit(amount, destination, data)
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
        data: String?,
    ): Result<TransactionFee.Choosable> {
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
}