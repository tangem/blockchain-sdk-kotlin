package com.tangem.blockchain.blockchains.algorand

import android.util.Log
import com.tangem.blockchain.blockchains.algorand.models.AlgorandAccountModel
import com.tangem.blockchain.blockchains.algorand.models.AlgorandTransactionInfo
import com.tangem.blockchain.blockchains.algorand.network.AlgorandNetworkService
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.extensions.toSimpleFailure
import com.tangem.common.CompletionResult
import java.math.BigDecimal
import java.util.EnumSet

internal class AlgorandWalletManager(
    wallet: Wallet,
    private val networkService: AlgorandNetworkService,
) : WalletManager(wallet) {

    override val currentHost: String get() = networkService.host
    private val transactionBuilder = AlgorandTransactionBuilder(
        blockchain = wallet.blockchain,
        publicKey = wallet.publicKey.blockchainKey,
    )

    override suspend fun updateInternal() {
        val pendingTxs = wallet.recentTransactions.mapNotNullTo(hashSetOf(), TransactionData::hash)
        when (val result = networkService.getAccountInfo(wallet.address, pendingTxs)) {
            is Result.Success -> updateWallet(result.data)
            is Result.Failure -> updateError(result.error)
        }
    }

    private fun updateWallet(accountModel: AlgorandAccountModel) {
        wallet.setCoinValue(accountModel.coinValue)
        wallet.setReserveValue(accountModel.reserveValue)
        accountModel.transactionsInfo
            .asSequence()
            .filter { it.status != AlgorandTransactionInfo.Status.STILL }
            .mapNotNullTo(hashSetOf(), AlgorandTransactionInfo::transactionHash)
            .forEach { txHash ->
                wallet.recentTransactions.find { txHash.equals(it.hash, ignoreCase = true) }?.let { transactionData ->
                    transactionData.status = TransactionStatus.Confirmed
                }
            }

        if (accountModel.coinValue < accountModel.existentialDeposit) {
            updateError(BlockchainSdkError.AccountNotFound(amountToCreateAccount = accountModel.reserveValue))
        }
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) throw error
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val txParams = networkService.getTransactionParams().successOr { return it.toSimpleFailure() }
        val txToSign = transactionBuilder.buildForSign(transactionData, txParams)
        return when (val signatureResult = signer.sign(txToSign, wallet.publicKey)) {
            is CompletionResult.Success -> {
                val rawTx =
                    transactionBuilder.buildForSend(
                        transactionData = transactionData,
                        params = txParams,
                        signature = signatureResult.data,
                    )
                when (val sendResult = networkService.sendTransaction(rawTx)) {
                    is Result.Success -> {
                        transactionData.hash = sendResult.data
                        wallet.addOutgoingTransaction(transactionData, hashToLowercase = false)

                        SimpleResult.Success
                    }
                    is Result.Failure -> sendResult.toSimpleFailure()
                }
            }
            is CompletionResult.Failure -> SimpleResult.Failure(signatureResult.error.toBlockchainSdkError())
        }
    }

    override fun validateTransaction(amount: Amount, fee: Amount?): EnumSet<TransactionError> {
        val errors = super.validateTransaction(amount, fee)
        val totalSend = fee?.value?.add(amount.value) ?: amount.value ?: BigDecimal.ZERO
        val balance = wallet.getCoinAmount().value ?: BigDecimal.ZERO
        val reserve = wallet.amounts[AmountType.Reserve]?.value ?: BigDecimal.ZERO
        if (balance - totalSend < reserve) {
            errors.add(TransactionError.AmountLowerExistentialDeposit)
        }

        return errors
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        val feeParams = networkService.getEstimatedFee().successOr { return it }
        val targetFee = feeParams.fee.max(feeParams.minFee)
        val feeAmount = Amount(value = targetFee, blockchain = wallet.blockchain)
        return Result.Success(
            TransactionFee.Single(
                Fee.Common(amount = feeAmount),
            ),
        )
    }
}