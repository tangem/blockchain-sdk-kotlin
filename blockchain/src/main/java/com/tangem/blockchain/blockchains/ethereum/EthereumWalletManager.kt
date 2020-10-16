package com.tangem.blockchain.blockchains.ethereum

import android.util.Log
import com.tangem.blockchain.blockchains.ethereum.network.EthereumInfoResponse
import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkManager
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.commands.SignResponse
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import org.kethereum.keccakshortcut.keccak
import java.math.BigDecimal

class EthereumWalletManager(
        cardId: String,
        wallet: Wallet,
        private val transactionBuilder: EthereumTransactionBuilder,
        private val networkManager: EthereumNetworkManager
) : WalletManager(cardId, wallet), TransactionSender, SignatureCountValidator {

    private val blockchain = wallet.blockchain

    private var pendingTxCount = -1L
    private var txCount = -1L

    override suspend fun update() {

        val result = networkManager.getInfo(
                wallet.address,
                wallet.amounts[AmountType.Token]?.address,
                wallet.amounts[AmountType.Token]?.decimals
        )
        when (result) {
            is Result.Failure -> updateError(result.error)
            is Result.Success -> updateWallet(result.data)
        }
    }

    private fun updateWallet(data: EthereumInfoResponse) {
        wallet.amounts[AmountType.Coin]?.value = data.balance
        wallet.amounts[AmountType.Token]?.value = data.tokenBalance
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
    ): Result<SignResponse> {
        val transactionToSign = transactionBuilder.buildToSign(transactionData, txCount.toBigInteger())
                ?: return Result.Failure(Exception("Not enough data"))
        when (val signerResponse = signer.sign(transactionToSign.hashes.toTypedArray(), cardId)) {
            is CompletionResult.Success -> {
                val transactionToSend = transactionBuilder.buildToSend(signerResponse.data.signature, transactionToSign)
                val sendResult = networkManager.sendTransaction(String.format("0x%s", transactionToSend.toHexString()))

                if (sendResult is SimpleResult.Success) {
                    transactionData.hash = transactionToSend.keccak().toHexString()
                    wallet.addOutgoingTransaction(transactionData)
                }
                return sendResult.toResultWithData(signerResponse.data)
            }
            is CompletionResult.Failure -> return Result.failure(signerResponse.error)
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        val result = networkManager.getFee(getGasLimit(amount).value)
        when (result) {
            is Result.Success -> {
                val feeValues: List<BigDecimal> = result.data
                return Result.Success(
                        feeValues.map { feeValue -> Amount(wallet.amounts[AmountType.Coin]!!, feeValue) })
            }
            is Result.Failure -> return result
        }
    }

    override suspend fun validateSignatureCount(signedHashes: Int): SimpleResult {
        return when (val result = networkManager.getSignatureCount(wallet.address)) {
            is Result.Success -> if (result.data == signedHashes) {
                SimpleResult.Success
            } else {
                SimpleResult.Failure(BlockchainSdkError.SignatureCountNotMatched)
            }
            is Result.Failure -> SimpleResult.Failure(result.error)
        }
    }
}