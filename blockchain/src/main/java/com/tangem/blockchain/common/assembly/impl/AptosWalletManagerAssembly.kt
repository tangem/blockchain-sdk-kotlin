package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.aptos.AptosNetworkProvidersBuilder
import com.tangem.blockchain.blockchains.aptos.AptosWalletManager
import com.tangem.blockchain.blockchains.aptos.network.AptosNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object AptosWalletManagerAssembly : WalletManagerAssembly<AptosWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): AptosWalletManager {
        return AptosWalletManager(
            wallet = input.wallet,
            networkService = AptosNetworkService(
                providers = AptosNetworkProvidersBuilder(
                    blockchain = input.wallet.blockchain,
                    config = input.config,
                ).build(),
            ),
        )
    }
}
