package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.aptos.AptosProvidersBuilder
import com.tangem.blockchain.blockchains.aptos.AptosWalletManager
import com.tangem.blockchain.blockchains.aptos.network.AptosNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object AptosWalletManagerAssembly : WalletManagerAssembly<AptosWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): AptosWalletManager {
        return with(input) {
            AptosWalletManager(
                wallet = wallet,
                networkService = AptosNetworkService(
                    providers = AptosProvidersBuilder(providerTypes = input.providerTypes, config = config)
                        .build(blockchain = wallet.blockchain),
                    blockchain = wallet.blockchain,
                ),
            )
        }
    }
}