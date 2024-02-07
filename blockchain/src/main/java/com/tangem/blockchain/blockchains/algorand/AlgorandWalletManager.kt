package com.tangem.blockchain.blockchains.algorand

import android.util.Log
import com.tangem.blockchain.blockchains.algorand.models.AlgorandAccountModel
import com.tangem.blockchain.blockchains.algorand.network.AlgorandNetworkService
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult

internal class AlgorandWalletManager(
    wallet: Wallet,
    private val networkService: AlgorandNetworkService,
) : WalletManager(wallet) {
    override val currentHost: String get() = networkService.host

    override suspend fun updateInternal() {
        when (val result = networkService.getAccount(wallet.address)) {
            is Result.Success -> updateWallet(result.data)
            is Result.Failure -> updateError(result.error)
        }
    }

    private fun updateWallet(accountModel: AlgorandAccountModel) {
        wallet.setCoinValue(accountModel.coinValue)
        wallet.setReserveValue(accountModel.reserveValue)

        if (accountModel.coinValue < accountModel.existentialDeposit) {
            updateError(BlockchainSdkError.AccountNotFound)
        }
    }

    private fun updateError(error: BlockchainError) {
        Log.e(this::class.java.simpleName, error.customMessage)
        if (error is BlockchainSdkError) throw error
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        TODO("Not yet implemented")
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        TODO("Not yet implemented")
    }
}