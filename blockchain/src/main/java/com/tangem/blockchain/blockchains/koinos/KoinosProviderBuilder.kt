package com.tangem.blockchain.blockchains.koinos

import com.tangem.blockchain.blockchains.koinos.network.KoinosNetworkProvier
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType

internal class KoinosProviderBuilder(
    override val providerTypes: List<ProviderType> = emptyList(),
) : NetworkProvidersBuilder<KoinosNetworkProvier>() {

    override fun createProviders(blockchain: Blockchain): List<KoinosNetworkProvier> {
        return listOf(
            createProvider(baseUrl = "https://api.koinos.io/", isTestnet = false),
            createProvider(baseUrl = "https://api.koinosblocks.com/", isTestnet = false),
        )
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<KoinosNetworkProvier> {
        return listOf(
            createProvider(baseUrl = "https://harbinger-api.koinos.io/", isTestnet = true),
            createProvider(baseUrl = "https://testnet.koinosblocks.com/", isTestnet = true),
        )
    }

    private fun createProvider(baseUrl: String, isTestnet: Boolean, apiKey: String? = null): KoinosNetworkProvier {
        return KoinosNetworkProvier(baseUrl = baseUrl, isTestnet = isTestnet, apiKey = apiKey)
    }
}