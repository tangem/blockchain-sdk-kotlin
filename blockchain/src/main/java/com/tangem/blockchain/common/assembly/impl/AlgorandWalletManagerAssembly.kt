package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.algorand.AlgorandNetworkProvidersBuilder
import com.tangem.blockchain.blockchains.algorand.AlgorandWalletManager
import com.tangem.blockchain.blockchains.algorand.network.AlgorandNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.txhistory.getTransactionHistoryProvider

internal object AlgorandWalletManagerAssembly : WalletManagerAssembly<AlgorandWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): AlgorandWalletManager {
        val blockchain = input.wallet.blockchain
        return AlgorandWalletManager(
            wallet = input.wallet,
            networkService = AlgorandNetworkService(
                networkProviders = AlgorandNetworkProvidersBuilder(
                    blockchain = blockchain,
                    config = input.config,
                ).build(),
                blockchain = blockchain,
            ),
            transactionHistoryProvider = blockchain.getTransactionHistoryProvider(input.config),
        )
    }
}