package com.tangem.blockchain.blockchains.binance

import android.util.Log
import com.tangem.blockchain.blockchains.binance.network.BinanceInfoResponse
import com.tangem.blockchain.blockchains.binance.network.BinanceNetworkService
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.commands.SignResponse
import com.tangem.common.CompletionResult
import java.math.BigDecimal

class BinanceWalletManager(
        cardId: String,
        wallet: Wallet,
        private val transactionBuilder: BinanceTransactionBuilder,
        private val networkService: BinanceNetworkService,
        presetTokens: Set<Token>
) : WalletManager(cardId, wallet, presetTokens), TransactionSender {

    private val blockchain = wallet.blockchain

    override suspend fun update() {
        val result = networkService.getInfo(wallet.address)
        when (result) {
            is Result.Success -> updateWallet(result.data)
            is Result.Failure -> updateError(result.error)
        }
    }

    private fun updateWallet(response: BinanceInfoResponse) {
        val coinBalance = response.balances[blockchain.currency] ?: 0.toBigDecimal()
        Log.d(this::class.java.simpleName, "Balance is $coinBalance")
        wallet.setCoinValue(coinBalance)

        presetTokens.forEach {
            val tokenBalance = response.balances[it.contractAddress] ?: 0.toBigDecimal()
            wallet.setTokenValue(tokenBalance, it)
        }
        if (presetTokens.isEmpty()) { // only if no token(s) specified on manager creation or stored on card
            val tokenBalances = response.balances.filterKeys { it != blockchain.currency}
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
                    decimals = blockchain.decimals()
            )
            wallet.setTokenValue(it.value, token)
        }
    }

    private fun updateError(error: Throwable?) {
        Log.e(this::class.java.simpleName, error?.message ?: "")
        if (error != null) throw error
    }

    override suspend fun send(
            transactionData: TransactionData, signer: TransactionSigner
    ): Result<SignResponse> {
        val buildTransactionResult = transactionBuilder.buildToSign(transactionData)
        return when (buildTransactionResult) {
            is Result.Failure -> Result.Failure(buildTransactionResult.error)
            is Result.Success -> {
                when (val signerResponse = signer.sign(arrayOf(buildTransactionResult.data), cardId)) {
                    is CompletionResult.Success -> {
                        val transactionToSend = transactionBuilder.buildToSend(signerResponse.data.signature)
                        networkService.sendTransaction(transactionToSend)
                                .toResultWithData(signerResponse.data)
                    }
                    is CompletionResult.Failure -> Result.failure(signerResponse.error)
                }
            }
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        when (val result = networkService.getFee()) {
            is Result.Success -> return Result.Success(listOf(Amount(result.data, blockchain)))
            is Result.Failure -> return result
        }
    }


}