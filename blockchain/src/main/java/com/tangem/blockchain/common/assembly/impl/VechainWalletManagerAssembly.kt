package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.vechain.VechainNetworkProvidersBuilder
import com.tangem.blockchain.blockchains.vechain.VechainWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object VechainWalletManagerAssembly : WalletManagerAssembly<VechainWalletManager>() {
    override fun make(input: WalletManagerAssemblyInput): VechainWalletManager {
        return VechainWalletManager(
            wallet = input.wallet,
            networkProviders = VechainNetworkProvidersBuilder().build(input.wallet.blockchain.isTestnet(), input.config)
        )
    }
}