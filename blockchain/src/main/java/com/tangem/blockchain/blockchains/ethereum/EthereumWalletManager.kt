package com.tangem.blockchain.blockchains.ethereum

import android.util.Log
import com.tangem.blockchain.blockchains.ethereum.network.EthereumInfoResponse
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.kethereum.keccakshortcut.keccak
import java.math.BigDecimal
import java.math.RoundingMode

class EthereumWalletManager(
    wallet: Wallet,
    private val transactionBuilder: EthereumTransactionBuilder,
    private val networkProvider: EthereumNetworkProvider,
    presetToken: MutableSet<Token>,
) : WalletManager(wallet, presetToken),
    TransactionSender, SignatureCountValidator, TokenFinder, EthereumGasLoader {

    private var pendingTxCount = -1L
    private var txCount = -1L

    override val currentHost: String
        get() = networkProvider.host

    override suspend fun update() {

        when (val result = networkProvider.getInfo(wallet.address, cardTokens)) {
            is Result.Failure -> updateError(result.error)
            is Result.Success -> updateWallet(result.data)
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

    private fun updateError(error: Throwable?) {
        Log.e(this::class.java.simpleName, error?.message ?: "")
        if (error != null) throw error
    }

    override suspend fun send(
        transactionData: TransactionData, signer: TransactionSigner,
    ): SimpleResult {
        val transactionToSign =
            transactionBuilder.buildToSign(transactionData, txCount.toBigInteger())
                ?: return SimpleResult.Failure(Exception("Not enough data"))

        val signerResponse = signer.sign(transactionToSign.hash,wallet.cardId, wallet.publicKey)
        return when (signerResponse) {
            is CompletionResult.Success -> {
                val transactionToSend = transactionBuilder
                    .buildToSend(signerResponse.data, transactionToSign)
                val sendResult = networkProvider
                    .sendTransaction("0x" + transactionToSend.toHexString())

                if (sendResult is SimpleResult.Success) {
                    transactionData.hash = transactionToSend.keccak().toHexString()
                    wallet.addOutgoingTransaction(transactionData)
                }
                sendResult
            }
            is CompletionResult.Failure -> SimpleResult.fromTangemSdkError(signerResponse.error)
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        return try {
            coroutineScope {
                val gasLimitResponsesDeferred =
                    async { getGasLimit(amount, destination) }
                val gasPriceResponsesDeferred = async { getGasPrice() }

                val gasLimit = when (val gasLimitResult = gasLimitResponsesDeferred.await()) {
                    is Result.Failure -> return@coroutineScope Result.Failure(gasLimitResult.error)
                    is Result.Success -> gasLimitResult.data
                }
                val gasPrice = when (val gasPriceResult = gasPriceResponsesDeferred.await()) {
                    is Result.Failure -> return@coroutineScope Result.Failure(gasPriceResult.error)
                    is Result.Success -> gasPriceResult.data
                }

                val fees = calculateFees(gasPrice, gasLimit)
                    .map { value -> Amount(wallet.amounts[AmountType.Coin]!!, value) }
                Result.Success(fees)
            }
        } catch (error: Exception) {
            Result.Failure(error)
        }
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

    override suspend fun addToken(token: Token): Result<Amount> {
        if (!cardTokens.contains(token)) {
            cardTokens.add(token)
        }
        return when (val result = networkProvider.getTokensBalance(wallet.address, setOf(token))) {
            is Result.Success -> Result.Success(wallet.addTokenValue(result.data[token]!!, token))
            is Result.Failure -> Result.Failure(result.error)
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

    override suspend fun getGasPrice(): Result<Long> {
        return networkProvider.getGasPrice()
    }

    override suspend fun getGasLimit(amount: Amount, destination: String): Result<Long> {
        var to = destination
        val from = wallet.address
        var data: String? = null

        if (amount.type is AmountType.Token) {
            to = amount.type.token.contractAddress
            data =
                "0x" + EthereumUtils.createErc20TransferData(destination, amount).toHexString()
        }

        return when (val result =  networkProvider.getGasLimit(to, from, data) ){
            is Result.Failure -> result
            is Result.Success -> {
                transactionBuilder.gasLimit = result.data.toBigInteger()
                result
            }
        }
    }

    private fun calculateFees(gasPrice: Long, gasLimit: Long): List<BigDecimal> {
        val minFee = gasPrice.toBigDecimal().multiply(gasLimit.toBigDecimal())
        val normalFee = minFee.multiply(BigDecimal(1.2)).setScale(0, RoundingMode.HALF_UP)
        val priorityFee = minFee.multiply(BigDecimal(1.5)).setScale(0, RoundingMode.HALF_UP)
        return listOf(
            minFee.movePointLeft(Blockchain.Ethereum.decimals()),
            normalFee.movePointLeft(Blockchain.Ethereum.decimals()),
            priorityFee.movePointLeft(Blockchain.Ethereum.decimals())
        )
    }
}