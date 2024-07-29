package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.icp.InternetComputerProvidersBuilder
import com.tangem.blockchain.blockchains.icp.InternetComputerWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object InternetComputerWalletManagerAssembly : WalletManagerAssembly<InternetComputerWalletManager>() {
    override fun make(input: WalletManagerAssemblyInput): InternetComputerWalletManager {
        return with(input.wallet) {
            InternetComputerWalletManager(
                wallet = this,
                networkProviders = InternetComputerProvidersBuilder(input.providerTypes).build(blockchain)
            )
        }
    }
}
