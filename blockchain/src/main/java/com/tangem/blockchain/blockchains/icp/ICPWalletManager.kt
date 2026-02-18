package com.tangem.blockchain.blockchains.icp

import android.util.Log
import com.tangem.blockchain.blockchains.icp.network.ICPNetworkProvider
import com.tangem.blockchain.blockchains.icp.network.ICPTransferWithSigner
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.memo.MemoState
import com.tangem.blockchain.common.memo.isValidUInt64
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import java.math.BigDecimal

internal class ICPWalletManager(
    wallet: Wallet,
    private val networkProvider: ICPNetworkProvider,
    private val transactionBuilder: ICPTransactionBuilder,
) : WalletManager(wallet), TransactionValidator {

    override val currentHost: String get() = networkProvider.baseUrl
    val blockchain = wallet.blockchain

    override suspend fun updateInternal() {
        when (val result = networkProvider.getBalance(wallet.address)) {
            is Result.Success -> updateWallet(result.data)
            is Result.Failure -> updateError(result.error)
        }
    }

    private fun updateWallet(balance: BigDecimal) {
        if (balance != wallet.amounts[AmountType.Coin]?.value) {
            wallet.recentTransactions.clear()
        }
        wallet.setCoinValue(balance)
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage, error)
        if (error is BlockchainSdkError) throw error
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        val nowMillis = System.currentTimeMillis()
        val nowNanos = nowMillis * NANOSECONDS_IN_MILLISECOND
        val transferRequest = transactionBuilder.buildForSign(transactionData, nowNanos)
        val transferWithSigner = ICPTransferWithSigner(transferRequest, signer)

        return when (val sendResult = networkProvider.signAndSendTransaction(transferWithSigner)) {
            is Result.Success -> {
                val txHash = sendResult.data?.toString() ?: ""
                wallet.addOutgoingTransaction(
                    transactionData = transactionData,
                    txHash = txHash,
                    hashToLowercase = false,
                )

                Result.Success(TransactionSendResult(txHash))
            }
            is Result.Failure -> sendResult
        }
    }

    override suspend fun validate(transactionData: TransactionData): kotlin.Result<Unit> {
        return kotlin.Result.success(Unit)
    }

    override suspend fun validateMemo(memo: String): Result<MemoState> {
        if (memo.isEmpty()) return Result.Success(MemoState.Valid)
        val isValid = memo.isValidUInt64()
        return Result.Success(if (isValid) MemoState.Valid else MemoState.Invalid)
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        return Result.Success(TransactionFee.Single(Fee.Common(Amount(FEE, blockchain))))
    }

    companion object {
        private val FEE = 0.0001.toBigDecimal()
        private const val NANOSECONDS_IN_MILLISECOND = 1000000
    }
}