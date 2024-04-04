package com.tangem.blockchain.blockchains.polkadot.providers

import com.tangem.blockchain.blockchains.polkadot.network.PolkadotCombinedProvider
import com.tangem.blockchain.blockchains.polkadot.network.PolkadotNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder

internal class KusamaProvidersBuilder : NetworkProvidersBuilder<PolkadotNetworkProvider>() {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Kusama)

    override fun createProviders(blockchain: Blockchain): List<PolkadotNetworkProvider> {
        return listOf(
            "https://kusama-rpc.polkadot.io/",
            "https://kusama.api.onfinality.io/public-ws/",
            "https://kusama-rpc.dwellir.com/",
        )
            .map { PolkadotCombinedProvider(baseUrl = it, blockchain = blockchain) }
    }
}