package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.polkadot.PolkadotWalletManager
import com.tangem.blockchain.blockchains.polkadot.extensions.getPolkadotExtrinsicCheckHost
import com.tangem.blockchain.blockchains.polkadot.extensions.getPolkadotHosts
import com.tangem.blockchain.blockchains.polkadot.network.PolkadotCombinedProvider
import com.tangem.blockchain.blockchains.polkadot.network.PolkadotNetworkService
import com.tangem.blockchain.blockchains.polkadot.network.accounthealthcheck.PolkadotAccountHealthCheckNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object PolkadotWalletManagerAssembly : WalletManagerAssembly<PolkadotWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): PolkadotWalletManager {
        val healthCheckService = input.wallet.blockchain.getPolkadotExtrinsicCheckHost()?.let {
            PolkadotAccountHealthCheckNetworkService(it)
        }
        return PolkadotWalletManager(
            wallet = input.wallet,
            networkProvider = PolkadotNetworkService(
                providers = input.wallet.blockchain.getPolkadotHosts()
                    .map { PolkadotCombinedProvider(input.wallet.blockchain.decimals(), it) },
            ),
            extrinsicCheckNetworkProvider = healthCheckService,
        )
    }
}