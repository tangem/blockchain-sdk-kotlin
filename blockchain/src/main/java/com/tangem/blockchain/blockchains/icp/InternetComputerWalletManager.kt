package com.tangem.blockchain.blockchains.icp

import com.tangem.blockchain.blockchains.icp.network.InternetComputerNetworkProvider
import com.tangem.blockchain.blockchains.icp.network.InternetComputerNetworkService
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result

internal class InternetComputerWalletManager(
    wallet: Wallet,
    networkProviders: List<InternetComputerNetworkProvider>,
) : WalletManager(wallet) {

    private val networkService = InternetComputerNetworkService(networkProviders = networkProviders)
    override val currentHost: String get() = networkService.host

    override suspend fun updateInternal() {
        networkService.getBalance(wallet.address)
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        return Result.Failure(BlockchainSdkError.FailedToSendException)
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        return Result.Failure(BlockchainSdkError.FailedToLoadFee)
    }
}