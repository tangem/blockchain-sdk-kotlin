package com.tangem.blockchain.blockchains.factorn

import com.tangem.blockchain.blockchains.factorn.network.Fact0rnNetworkService
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.electrum.ElectrumNetworkProvider

internal class Fact0rnWalletManager(
    wallet: Wallet,
    networkProviders: List<ElectrumNetworkProvider>,
) : WalletManager(wallet) {

    private val networkService = Fact0rnNetworkService(networkProviders)

    override val currentHost: String get() = networkService.baseUrl

    override suspend fun updateInternal() {
        TODO("Not yet implemented")
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        TODO("Not yet implemented")
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        TODO("Not yet implemented")
    }
}