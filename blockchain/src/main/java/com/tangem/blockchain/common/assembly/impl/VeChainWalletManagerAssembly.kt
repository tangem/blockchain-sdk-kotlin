package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.vechain.VeChainProvidersBuilder
import com.tangem.blockchain.blockchains.vechain.VeChainWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object VeChainWalletManagerAssembly : WalletManagerAssembly<VeChainWalletManager>() {
    override fun make(input: WalletManagerAssemblyInput): VeChainWalletManager {
        return with(input) {
            VeChainWalletManager(
                wallet = wallet,
                networkProviders = VeChainProvidersBuilder(input.providerTypes, config).build(wallet.blockchain),
            )
        }
    }
}