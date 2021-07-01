package com.tangem.blockchain.blockchains.binance

import android.util.Log
import com.tangem.blockchain.blockchains.binance.network.BinanceInfoResponse
import com.tangem.blockchain.blockchains.binance.network.BinanceNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import java.math.BigDecimal

class BinanceWalletManager(
        wallet: Wallet,
        private val transactionBuilder: BinanceTransactionBuilder,
        private val networkProvider: BinanceNetworkProvider,
        presetTokens: MutableSet<Token>
) : WalletManager(wallet, presetTokens), TransactionSender {

    private val blockchain = wallet.blockchain

    override val currentHost: String
        get() = networkProvider.host

    override suspend fun update() {
        val result = networkProvider.getInfo(wallet.address)
        when (result) {
            is Result.Success -> updateWallet(result.data)
            is Result.Failure -> updateError(result.error)
        }
    }

    private fun updateWallet(response: BinanceInfoResponse) {
        val coinBalance = response.balances[blockchain.currency] ?: 0.toBigDecimal()
        Log.d(this::class.java.simpleName, "Balance is $coinBalance")
        wallet.setCoinValue(coinBalance)

        cardTokens.forEach {
            val tokenBalance = response.balances[it.contractAddress] ?: 0.toBigDecimal()
            wallet.addTokenValue(tokenBalance, it)
        }
        if (cardTokens.isEmpty()) { // only if no token(s) specified on manager creation or stored on card
            val tokenBalances = response.balances.filterKeys { it != blockchain.currency }
            updateUnplannedTokens(tokenBalances)
        }

        transactionBuilder.accountNumber = response.accountNumber
        transactionBuilder.sequence = response.sequence
    }

    private fun updateUnplannedTokens(balances: Map<String, BigDecimal>) {
        balances.forEach {
            val token = Token(
                    symbol = it.key.split("-")[0],
                    contractAddress = it.key,
                    decimals = blockchain.decimals(),
                    blockchain = blockchain
            )
            wallet.addTokenValue(it.value, token)
        }
    }

    private fun updateError(error: Throwable?) {
        Log.e(this::class.java.simpleName, error?.message ?: "")
        if (error != null) throw error
    }

    override suspend fun send(
            transactionData: TransactionData, signer: TransactionSigner
    ): SimpleResult {
        val buildTransactionResult = transactionBuilder.buildToSign(transactionData)
        return when (buildTransactionResult) {
            is Result.Failure -> SimpleResult.Failure(buildTransactionResult.error)
            is Result.Success -> {
                val signerResponse = signer.sign(
                        buildTransactionResult.data,
                        wallet.cardId, walletPublicKey = wallet.publicKey
                )
                when (signerResponse) {
                    is CompletionResult.Success -> {
                        val transactionToSend = transactionBuilder.buildToSend(signerResponse.data)
                        networkProvider.sendTransaction(transactionToSend)
                    }
                    is CompletionResult.Failure -> SimpleResult.failure(signerResponse.error)
                }
            }
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        when (val result = networkProvider.getFee()) {
            is Result.Success -> return Result.Success(listOf(Amount(result.data, blockchain)))
            is Result.Failure -> return result
        }
    }


}