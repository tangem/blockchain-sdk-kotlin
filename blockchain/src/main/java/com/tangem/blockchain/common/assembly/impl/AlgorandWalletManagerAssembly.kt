package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.algorand.AlgorandProvidersBuilder
import com.tangem.blockchain.blockchains.algorand.AlgorandWalletManager
import com.tangem.blockchain.blockchains.algorand.network.AlgorandNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.txhistory.getTransactionHistoryProvider

internal object AlgorandWalletManagerAssembly : WalletManagerAssembly<AlgorandWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): AlgorandWalletManager {
        return with(input) {
            AlgorandWalletManager(
                wallet = wallet,
                networkService = AlgorandNetworkService(
                    networkProviders = AlgorandProvidersBuilder(providerTypes = input.providerTypes, config = config)
                        .build(blockchain = wallet.blockchain),
                    blockchain = wallet.blockchain,
                ),
                transactionHistoryProvider = wallet.blockchain.getTransactionHistoryProvider(config),
            )
        }
    }
}