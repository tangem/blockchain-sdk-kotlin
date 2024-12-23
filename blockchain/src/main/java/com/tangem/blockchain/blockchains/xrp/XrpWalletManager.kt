package com.tangem.blockchain.blockchains.xrp

import android.util.Log
import com.tangem.blockchain.blockchains.xrp.network.XrpInfoResponse
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import java.math.BigDecimal

class XrpWalletManager(
    wallet: Wallet,
    private val transactionBuilder: XrpTransactionBuilder,
    private val networkProvider: XrpNetworkProvider,
) : WalletManager(wallet), ReserveAmountProvider {

    override val currentHost: String
        get() = networkProvider.baseUrl

    private val blockchain = wallet.blockchain

    override suspend fun updateInternal() {
        when (val result = networkProvider.getInfo(wallet.address)) {
            is Result.Success -> updateWallet(result.data)
            is Result.Failure -> updateError(result.error)
        }
    }

    private fun updateWallet(response: XrpInfoResponse) {
        Log.d(this::class.java.simpleName, "Balance is ${response.balance}")

        wallet.setReserveValue(response.reserveTotal)
        if (!response.accountFound) {
            updateError(BlockchainSdkError.AccountNotFound(response.reserveTotal))
            return
        }
        wallet.setCoinValue(response.balance - response.reserveTotal)
        transactionBuilder.sequence = response.sequence
        transactionBuilder.minReserve = response.reserveBase

        if (response.hasUnconfirmed) {
            if (wallet.recentTransactions.isEmpty()) wallet.addTransactionDummy()
        } else {
            wallet.recentTransactions.forEach { it.status = TransactionStatus.Confirmed }
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
        val transactionHash = when (val buildResult = transactionBuilder.buildToSign(transactionData)) {
            is Result.Success -> buildResult.data
            is Result.Failure -> return Result.Failure(buildResult.error)
        }

        return when (val signerResponse = signer.sign(transactionHash, wallet.publicKey)) {
            is CompletionResult.Success -> {
                val transactionToSend = transactionBuilder.buildToSend(signerResponse.data)
                when (val sendResult = networkProvider.sendTransaction(transactionToSend)) {
                    is SimpleResult.Failure -> Result.Failure(sendResult.error)
                    SimpleResult.Success -> {
                        val hash = transactionBuilder.getTransactionHash()?.toHexString()
                        transactionData.hash = hash
                        wallet.addOutgoingTransaction(transactionData)
                        Result.Success(TransactionSendResult(hash ?: ""))
                    }
                }
            }
            is CompletionResult.Failure -> Result.fromTangemSdkError(signerResponse.error)
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        return when (val result = networkProvider.getFee()) {
            is Result.Failure -> result
            is Result.Success -> Result.Success(
                TransactionFee.Choosable(
                    minimum = Fee.Common(Amount(result.data.minimalFee, blockchain)),
                    normal = Fee.Common(Amount(result.data.normalFee, blockchain)),
                    priority = Fee.Common(Amount(result.data.priorityFee, blockchain)),
                ),
            )
        }
    }

    override fun getReserveAmount(): BigDecimal = transactionBuilder.minReserve

    override suspend fun isAccountFunded(destinationAddress: String): Boolean {
        return networkProvider.checkIsAccountCreated(destinationAddress)
    }
}