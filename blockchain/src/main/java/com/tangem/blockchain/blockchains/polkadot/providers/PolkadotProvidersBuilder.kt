package com.tangem.blockchain.blockchains.polkadot.providers

import com.tangem.blockchain.blockchains.polkadot.network.PolkadotCombinedProvider
import com.tangem.blockchain.blockchains.polkadot.network.PolkadotNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder

internal class PolkadotProvidersBuilder : NetworkProvidersBuilder<PolkadotNetworkProvider>() {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Polkadot, Blockchain.PolkadotTestnet)

    override fun createProviders(blockchain: Blockchain): List<PolkadotNetworkProvider> {
        return if (blockchain.isTestnet()) {
            listOf("https://westend-rpc.polkadot.io/")
        } else {
            listOf(
                "https://rpc.polkadot.io/",
                "https://polkadot.api.onfinality.io/public-ws/",
                "https://polkadot-rpc.dwellir.com/",
            )
        }
            .map { PolkadotCombinedProvider(baseUrl = it, blockchain = blockchain) }
    }
}