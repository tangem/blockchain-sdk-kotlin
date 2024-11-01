package com.tangem.blockchain.blockchains.casper

import android.util.Log
import com.tangem.blockchain.blockchains.casper.models.CasperBalance
import com.tangem.blockchain.blockchains.casper.network.CasperNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import java.math.BigDecimal

internal class CasperWalletManager(
    wallet: Wallet,
    private val networkProvider: CasperNetworkProvider,
    private val transactionBuilder: CasperTransactionBuilder,
    private val curve: EllipticCurve,
) : WalletManager(wallet), MinimumSendAmountProvider {

    override val currentHost: String get() = networkProvider.baseUrl
    private val blockchain = wallet.blockchain

    override suspend fun updateInternal() {
        when (val result = networkProvider.getBalance(wallet.address)) {
            is Result.Success -> updateWallet(result.data)
            is Result.Failure -> updateError(result.error)
        }
    }

    private fun updateWallet(balance: CasperBalance) {
        if (balance.value != wallet.amounts[AmountType.Coin]?.value) {
            wallet.recentTransactions.clear()
        }
        wallet.setCoinValue(balance.value)
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage, error)
        if (error is BlockchainSdkError) throw error
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        return try {
            val unsignedBody = transactionBuilder.buildForSign(transactionData = transactionData)
            val hashSha256 = unsignedBody.hash.hexToBytes().calculateSha256()

            when (val signingResult = signer.sign(hashSha256, wallet.publicKey)) {
                is CompletionResult.Failure -> Result.fromTangemSdkError(signingResult.error)
                is CompletionResult.Success -> {
                    val signedTransactionBody = transactionBuilder.buildForSend(
                        unsignedTransactionBody = unsignedBody,
                        signature = encodeSignature(signingResult.data),
                    )

                    val result = networkProvider.putDeploy(body = signedTransactionBody)

                    when (result) {
                        is Result.Failure -> Result.Failure(result.error)
                        is Result.Success -> {
                            val txHash = result.data.deployHash
                            wallet.addOutgoingTransaction(transactionData.updateHash(hash = txHash))
                            transactionData.hash = txHash
                            Result.Success(TransactionSendResult(txHash))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(this::class.java.simpleName, e.toBlockchainSdkError().customMessage)
            throw e
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        return Result.Success(TransactionFee.Single(Fee.Common(Amount(FEE, blockchain))))
    }

    override fun getMinimumSendAmount(): BigDecimal = MIN_SEND_AMOUNT

    private fun encodeSignature(signature: ByteArray): ByteArray {
        return CasperConstants.getSignaturePrefix(curve).hexToBytes() + signature
    }

    companion object {
        // according to Casper Wallet
        private val FEE = 0.1.toBigDecimal()

        private val MIN_SEND_AMOUNT = 2.5.toBigDecimal()
    }
}