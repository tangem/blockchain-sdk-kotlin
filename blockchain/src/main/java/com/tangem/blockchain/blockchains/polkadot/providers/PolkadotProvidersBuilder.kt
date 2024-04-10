package com.tangem.blockchain.blockchains.polkadot.providers

import com.tangem.blockchain.blockchains.polkadot.network.PolkadotCombinedProvider
import com.tangem.blockchain.blockchains.polkadot.network.PolkadotNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder

internal class PolkadotProvidersBuilder : NetworkProvidersBuilder<PolkadotNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<PolkadotNetworkProvider> {
        return listOf(
            "https://rpc.polkadot.io/",
            "https://polkadot.api.onfinality.io/public-ws/",
            "https://polkadot-rpc.dwellir.com/",
        )
            .map { PolkadotCombinedProvider(baseUrl = it, blockchain = blockchain) }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<PolkadotNetworkProvider> {
        return listOf("https://westend-rpc.polkadot.io/")
            .map { PolkadotCombinedProvider(baseUrl = it, blockchain = blockchain) }
    }
}