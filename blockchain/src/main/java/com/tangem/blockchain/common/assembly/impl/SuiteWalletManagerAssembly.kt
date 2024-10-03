package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.sui.SuiNetworkProvidersBuilder
import com.tangem.blockchain.blockchains.sui.SuiWalletManager
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object SuiteWalletManagerAssembly : WalletManagerAssembly<WalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): WalletManager = with(input) {
        val providersBuilder = SuiNetworkProvidersBuilder(providerTypes)

        SuiWalletManager(
            wallet = wallet,
            networkProviders = providersBuilder.build(wallet.blockchain),
        )
    }
}