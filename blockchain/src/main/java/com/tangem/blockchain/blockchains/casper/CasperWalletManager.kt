package com.tangem.blockchain.blockchains.casper

import android.util.Log
import com.tangem.blockchain.blockchains.casper.models.CasperBalance
import com.tangem.blockchain.blockchains.casper.network.CasperNetworkProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import java.math.BigDecimal

internal class CasperWalletManager(
    wallet: Wallet,
    private val networkProvider: CasperNetworkProvider,
    private val transactionBuilder: CasperTransactionBuilder,
) : WalletManager(wallet), ReserveAmountProvider {

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
        TODO("Not yet implemented")
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        return Result.Success(TransactionFee.Single(Fee.Common(Amount(FEE, blockchain))))
    }

    override fun getReserveAmount(): BigDecimal = transactionBuilder.minReserve

    override suspend fun isAccountFunded(destinationAddress: String): Boolean =
        networkProvider.getBalance(destinationAddress) is Result.Success

    companion object {
        // according to Casper Wallet
        private val FEE = 0.1.toBigDecimal()
    }
}