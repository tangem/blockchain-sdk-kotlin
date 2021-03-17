package com.tangem.blockchain.blockchains.stellar

import android.util.Log
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.commands.SignResponse
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString

class StellarWalletManager(
        cardId: String,
        wallet: Wallet,
        private val transactionBuilder: StellarTransactionBuilder,
        private val networkProvider: StellarNetworkProvider,
        presetTokens: MutableSet<Token>
) : WalletManager(cardId, wallet, presetTokens), TransactionSender, SignatureCountValidator {

    private val blockchain = wallet.blockchain

    private var baseFee = BASE_FEE
    private var baseReserve = BASE_RESERVE
    private var sequence = 0L

    override suspend fun update() {
        when (val result = networkProvider.getInfo(wallet.address)) {
            is Result.Failure -> updateError(result.error)
            is Result.Success -> updateWallet(result.data)
        }
    }

    private fun updateWallet(data: StellarResponse) { // TODO: rework reserve
        val reserve = data.baseReserve * (2 + data.subEntryCount).toBigDecimal()
        wallet.setCoinValue(data.coinBalance - reserve)
        wallet.setReserveValue(reserve)
        sequence = data.sequence
        baseFee = data.baseFee
        baseReserve = data.baseReserve
        transactionBuilder.minReserve = data.baseReserve * 2.toBigDecimal()

        presetTokens.forEach { token ->
            val tokenBalance = data.tokenBalances
                    .find { it.symbol == token.symbol && it.issuer == token.contractAddress }?.balance
                    ?: 0.toBigDecimal()
            wallet.addTokenValue(tokenBalance, token)
        }
        // only if no token(s) specified on manager creation or stored on card
        if (presetTokens.isEmpty()) updateUnplannedTokens(data.tokenBalances)

        updateRecentTransactions(data.recentTransactions)
    }

    private fun updateUnplannedTokens(balances: Set<StellarAssetBalance>) {
        balances.forEach {
            val token = Token(it.symbol, it.issuer, blockchain.decimals())
            wallet.addTokenValue(it.balance, token)
        }
    }

    private fun updateError(error: Throwable?) {
        Log.e(this::class.java.simpleName, error?.message ?: "")
        if (error != null) throw error
    }

    override suspend fun send(
            transactionData: TransactionData, signer: TransactionSigner
    ): Result<SignResponse> {

        val hashes = when (val buildResult =
                transactionBuilder.buildToSign(transactionData, sequence)
        ) {
            is Result.Success -> listOf(buildResult.data)
            is Result.Failure -> return buildResult
        }
        return when (val signerResponse = signer.sign(hashes.toTypedArray(), cardId)) {
            is CompletionResult.Success -> {
                val transactionToSend = transactionBuilder.buildToSend(signerResponse.data.signature)
                val sendResult = networkProvider.sendTransaction(transactionToSend)

                if (sendResult is SimpleResult.Success) {
                    transactionData.hash = transactionBuilder.getTransactionHash().toHexString()
                    wallet.addOutgoingTransaction(transactionData)
                }
                sendResult.toResultWithData(signerResponse.data)
            }
            is CompletionResult.Failure -> Result.failure(signerResponse.error)
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        return Result.Success(listOf(
                Amount(baseFee, blockchain)
        ))
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

    companion object {
        val BASE_FEE = 0.00001.toBigDecimal()
        val BASE_RESERVE = 0.5.toBigDecimal()
    }
}
