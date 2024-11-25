package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.factorn.Fact0rnProvidersBuilder
import com.tangem.blockchain.blockchains.factorn.Fact0rnWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object Fact0rnWalletManagerAssembly : WalletManagerAssembly<Fact0rnWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): Fact0rnWalletManager {
        return with(input.wallet) {
            Fact0rnWalletManager(
                wallet = this,
                networkProviders = Fact0rnProvidersBuilder(input.providerTypes).build(blockchain),
            )
        }
    }
}