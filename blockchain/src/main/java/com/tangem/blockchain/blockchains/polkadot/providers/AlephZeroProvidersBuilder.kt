package com.tangem.blockchain.blockchains.polkadot.providers

import com.tangem.blockchain.blockchains.polkadot.network.PolkadotCombinedProvider
import com.tangem.blockchain.blockchains.polkadot.network.PolkadotNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder

internal class AlephZeroProvidersBuilder : NetworkProvidersBuilder<PolkadotNetworkProvider>() {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.AlephZero, Blockchain.AlephZeroTestnet)

    override fun createProviders(blockchain: Blockchain): List<PolkadotNetworkProvider> {
        return if (blockchain.isTestnet()) {
            listOf(
                "https://rpc.test.azero.dev",
            )
        } else {
            listOf(
                "https://rpc.azero.dev/",
                "https://aleph-zero-rpc.dwellir.com/",
            )
        }
            .map { PolkadotCombinedProvider(baseUrl = it, blockchain = blockchain) }
    }
}