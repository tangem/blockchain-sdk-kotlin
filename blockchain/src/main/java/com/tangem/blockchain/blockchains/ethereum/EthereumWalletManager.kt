package com.tangem.blockchain.blockchains.ethereum

import android.util.Log
import com.tangem.blockchain.blockchains.ethereum.network.EthereumInfoResponse
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.commands.SignResponse
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import org.kethereum.keccakshortcut.keccak
import java.math.BigDecimal

class EthereumWalletManager(
        wallet: Wallet,
        private val transactionBuilder: EthereumTransactionBuilder,
        private val networkProvider: EthereumNetworkProvider,
        presetToken: MutableSet<Token>,
) : WalletManager(wallet, presetToken),
        TransactionSender, SignatureCountValidator, TokenFinder {

    private var pendingTxCount = -1L
    private var txCount = -1L

    override suspend fun update() {

        when (val result = networkProvider.getInfo(wallet.address, presetTokens)) {
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
            transactionData: TransactionData, signer: TransactionSigner
    ): SimpleResult {
        val transactionToSign = transactionBuilder.buildToSign(transactionData, txCount.toBigInteger())
                ?: return SimpleResult.Failure(Exception("Not enough data"))
        val signerResponse = signer.sign(
                transactionToSign.hash,
                wallet.cardId, walletPublicKey = wallet.publicKey
        )
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
            is CompletionResult.Failure -> SimpleResult.failure(signerResponse.error)
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        var to = destination
        val from = wallet.address
        var data: String? = null
        val fallbackGasLimit = estimateGasLimit(amount).value

        if (amount.type is AmountType.Token) {
            to = amount.type.token.contractAddress
            data = "0x" + transactionBuilder.createErc20TransferData(destination, amount).toHexString()
        }

        return when (val result = networkProvider.getFee(to, from, data, fallbackGasLimit)) {
            is Result.Success -> {
                transactionBuilder.gasLimit = result.data.gasLimit.toBigInteger()
                val feeValues: List<BigDecimal> = result.data.fees
                Result.Success(
                        feeValues.map { feeValue -> Amount(wallet.amounts[AmountType.Coin]!!, feeValue) })
            }
            is Result.Failure -> result
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
        if (!presetTokens.contains(token)) {
            presetTokens.add(token)
        }
        val result = networkProvider.getTokensBalance(wallet.address, setOf(token))
        return when (result) {
            is Result.Failure -> Result.Failure(result.error)
            is Result.Success -> {
                val amount = wallet.addTokenValue(result.data[token]!!, token)
                Result.Success(amount)
            }
        }
    }

    override suspend fun findTokens(): Result<List<Token>> {
        return when (val result = networkProvider.findErc20Tokens(wallet.address)) {
            is Result.Failure -> Result.Failure(result.error)
            is Result.Success -> {
                val tokens: List<Token> = result.data.map { blockchairToken ->
                    val token = blockchairToken.toToken()
                    if (!presetTokens.contains(token)) {
                        presetTokens.add(token)
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

    private fun estimateGasLimit(amount: Amount): GasLimit { //TODO: remove?
        return if (amount.type == AmountType.Coin) {
            GasLimit.Default
        } else {
            when (amount.currencySymbol) {
                "DGX" -> GasLimit.High
                "AWG" -> GasLimit.Medium
                else -> GasLimit.Erc20
            }
        }
    }

    enum class GasLimit(val value: Long) {
        Default(21000),
        Erc20(60000),
        Medium(150000),
        High(300000)
    }
}