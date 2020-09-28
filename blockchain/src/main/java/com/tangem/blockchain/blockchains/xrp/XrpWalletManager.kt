package com.tangem.blockchain.blockchains.xrp

import android.util.Log
import com.tangem.blockchain.blockchains.xrp.network.XrpInfoResponse
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkManager
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.commands.SignResponse
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString

class XrpWalletManager(
        cardId: String,
        wallet: Wallet,
        private val transactionBuilder: XrpTransactionBuilder,
        private val networkManager: XrpNetworkManager
) : WalletManager(cardId, wallet), TransactionSender {

    private val blockchain = wallet.blockchain

    override suspend fun update() {
        val result = networkManager.getInfo(wallet.address)
        when (result) {
            is Result.Success -> updateWallet(result.data)
            is Result.Failure -> updateError(result.error)
        }
    }

    private fun updateWallet(response: XrpInfoResponse) {
        Log.d(this::class.java.simpleName, "Balance is ${response.balance}")

        if (!response.accountFound) {
            updateError(Exception("Account not found")) //TODO rework, add reserve
            return
        }
        wallet.setCoinValue(response.balance - response.reserveBase)
        wallet.setReserveValue(response.reserveBase)
        transactionBuilder.sequence = response.sequence
        transactionBuilder.minReserve = response.reserveBase

        if (response.hasUnconfirmed) {
            if (wallet.recentTransactions.isEmpty()) wallet.addIncomingTransactionDummy()
        } else {
            wallet.recentTransactions.forEach { it.status = TransactionStatus.Confirmed }
        }
    }

    private fun updateError(error: Throwable?) {
        Log.e(this::class.java.simpleName, error?.message ?: "")
        if (error != null) throw error
    }

    override suspend fun send(
            transactionData: TransactionData, signer: TransactionSigner
    ): Result<SignResponse> {
        val transactionHash =
                when (val buildResult = transactionBuilder.buildToSign(transactionData)) {
                    is Result.Success -> buildResult.data
                    is Result.Failure -> return Result.Failure(buildResult.error)
                }
        when (val signerResponse = signer.sign(arrayOf(transactionHash), cardId)) {
            is CompletionResult.Success -> {
                val transactionToSend = transactionBuilder.buildToSend(signerResponse.data.signature)
                val sendResult = networkManager.sendTransaction(transactionToSend)

                if (sendResult is SimpleResult.Success) {
                    transactionData.hash = transactionBuilder.getTransactionHash()?.toHexString()
                    wallet.addOutgoingTransaction(transactionData)
                }
                return sendResult.toResultWithData(signerResponse.data)
            }
            is CompletionResult.Failure -> return Result.failure(signerResponse.error)
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        val result = networkManager.getFee()
        when (result) {
            is Result.Failure -> return result
            is Result.Success -> return Result.Success(listOf(
                    Amount(result.data.minimalFee, blockchain),
                    Amount(result.data.normalFee, blockchain),
                    Amount(result.data.priorityFee, blockchain)
            ))
        }
    }
}