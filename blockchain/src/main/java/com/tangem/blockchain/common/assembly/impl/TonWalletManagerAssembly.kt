package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.ton.TonProvidersBuilder
import com.tangem.blockchain.blockchains.ton.TonWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object TonWalletManagerAssembly : WalletManagerAssembly<TonWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): TonWalletManager {
        return with(input.wallet) {
            TonWalletManager(
                wallet = this,
                networkProviders = TonProvidersBuilder(input.providerTypes, input.config).build(blockchain),
            )
        }
    }
}