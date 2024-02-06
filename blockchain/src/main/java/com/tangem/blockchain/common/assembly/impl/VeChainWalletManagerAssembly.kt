package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.vechain.VeChainNetworkProvidersBuilder
import com.tangem.blockchain.blockchains.vechain.VeChainWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object VeChainWalletManagerAssembly : WalletManagerAssembly<VeChainWalletManager>() {
    override fun make(input: WalletManagerAssemblyInput): VeChainWalletManager {
        return VeChainWalletManager(
            wallet = input.wallet,
            networkProviders = VeChainNetworkProvidersBuilder().build(
                isTestNet = input.wallet.blockchain.isTestnet(),
                config = input.config,
            ),
        )
    }
}
