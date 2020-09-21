package com.tangem.blockchain.blockchains.stellar

import android.util.Log
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import org.kethereum.keccakshortcut.keccak
import java.math.BigDecimal
import java.util.*

class StellarWalletManager(
        cardId: String,
        wallet: Wallet,
        private val transactionBuilder: StellarTransactionBuilder,
        private val networkManager: StellarNetworkManager
) : WalletManager(cardId, wallet), TransactionSender, SignatureCountValidator {

    private val blockchain = wallet.blockchain

    private var baseFee = BASE_FEE
    private var baseReserve = BASE_RESERVE
    private var sequence = 0L

    override suspend fun update() {
        val result = networkManager.getInfo(wallet.address, wallet.amounts[AmountType.Token]?.address)
        when (result) {
            is Result.Failure -> updateError(result.error)
            is Result.Success -> updateWallet(result.data)
        }
    }

    private fun updateWallet(data: StellarResponse) {
        val reserve = data.baseReserve * (2 + data.subEntryCount).toBigDecimal()
        wallet.setCoinValue(data.balance - reserve)
        wallet.setTokenValue(data.assetBalance ?: 0.toBigDecimal())
        wallet.setReserveValue(reserve)
        sequence = data.sequence
        baseFee = data.baseFee
        baseReserve = data.baseReserve
        updateRecentTransactions(data.recentTransactions)
    }

    private fun updateError(error: Throwable?) {
        Log.e(this::class.java.simpleName, error?.message ?: "")
        if (error != null) throw error
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val hashes = transactionBuilder.buildToSign(transactionData, sequence, baseFee.toStroops())
        when (val signerResponse = signer.sign(hashes.toTypedArray(), cardId)) {
            is CompletionResult.Success -> {
                val transactionToSend = transactionBuilder.buildToSend(signerResponse.data.signature)
                val sendResult = networkManager.sendTransaction(transactionToSend)

                if (sendResult is SimpleResult.Success) {
                    transactionData.hash = transactionBuilder.getTransactionHash().toHexString()
                    wallet.addOutgoingTransaction(transactionData)
                }
                return sendResult
            }
            is CompletionResult.Failure -> return SimpleResult.failure(signerResponse.error)
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        return Result.Success(listOf(
                Amount(baseFee, blockchain)
        ))
    }

    override suspend fun validateSignatureCount(signedHashes: Int): SimpleResult {
        return when (val result = networkManager.getSignatureCount(wallet.address)) {
            is Result.Success -> if (result.data == signedHashes) {
                SimpleResult.Success
            } else {
                SimpleResult.Failure(Exception("Number of signatures does not match"))
            }
            is Result.Failure -> SimpleResult.Failure(result.error)
        }
    }

    private fun BigDecimal.toStroops(): Int {
        return this.movePointRight(blockchain.decimals()).toInt()
    }

    companion object {
        val BASE_FEE = 0.00001.toBigDecimal()
        val BASE_RESERVE = 0.5.toBigDecimal()
    }
}
