package com.tangem.blockchain.blockchains.polkadot.providers

import com.tangem.blockchain.blockchains.polkadot.network.PolkadotCombinedProvider
import com.tangem.blockchain.blockchains.polkadot.network.PolkadotNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType

internal class AlephZeroProvidersBuilder(
    override val providerTypes: List<ProviderType>,
) : NetworkProvidersBuilder<PolkadotNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<PolkadotNetworkProvider> {
        return listOf(
            "https://rpc.azero.dev/",
            "https://aleph-zero-rpc.dwellir.com/",
        )
            .map { PolkadotCombinedProvider(baseUrl = it, blockchain = blockchain) }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<PolkadotNetworkProvider> {
        return listOf("https://rpc.test.azero.dev")
            .map { PolkadotCombinedProvider(baseUrl = it, blockchain = blockchain) }
    }
}