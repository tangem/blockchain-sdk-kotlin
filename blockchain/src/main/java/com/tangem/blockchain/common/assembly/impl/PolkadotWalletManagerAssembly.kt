package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.polkadot.PolkadotWalletManager
import com.tangem.blockchain.blockchains.polkadot.network.PolkadotNetworkProvider
import com.tangem.blockchain.blockchains.polkadot.network.PolkadotNetworkService
import com.tangem.blockchain.blockchains.polkadot.network.accounthealthcheck.PolkadotAccountHealthCheckNetworkService
import com.tangem.blockchain.blockchains.polkadot.providers.AlephZeroProvidersBuilder
import com.tangem.blockchain.blockchains.polkadot.providers.KusamaProvidersBuilder
import com.tangem.blockchain.blockchains.polkadot.providers.PolkadotProvidersBuilder
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder

internal object PolkadotWalletManagerAssembly : WalletManagerAssembly<PolkadotWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): PolkadotWalletManager {
        return with(input.wallet) {
            PolkadotWalletManager(
                wallet = this,
                networkProvider = PolkadotNetworkService(
                    providers = getNetworkProvidersBuilder(blockchain).build(blockchain),
                ),
                extrinsicCheckNetworkProvider = PolkadotAccountHealthCheckNetworkService(
                    baseUrl = "https://polkadot.api.subscan.io/",
                ),
            )
        }
    }

    private fun getNetworkProvidersBuilder(blockchain: Blockchain): NetworkProvidersBuilder<PolkadotNetworkProvider> {
        return when (blockchain) {
            Blockchain.Polkadot, Blockchain.PolkadotTestnet -> PolkadotProvidersBuilder()
            Blockchain.AlephZero, Blockchain.AlephZeroTestnet -> AlephZeroProvidersBuilder()
            Blockchain.Kusama -> KusamaProvidersBuilder()
            else -> error("$blockchain isn't supported")
        }
    }
}