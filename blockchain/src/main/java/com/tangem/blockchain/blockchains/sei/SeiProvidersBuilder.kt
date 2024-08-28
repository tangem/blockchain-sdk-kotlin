package com.tangem.blockchain.blockchains.sei

import com.tangem.blockchain.blockchains.cosmos.network.CosmosRestProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType

internal class SeiProvidersBuilder(
    override val providerTypes: List<ProviderType>,
) : NetworkProvidersBuilder<CosmosRestProvider>() {

    override fun createProviders(blockchain: Blockchain): List<CosmosRestProvider> {
        return providerTypes.mapNotNull {
            when (it) {
                is ProviderType.Public -> it.url
                else -> null
            }
        }
            .map(::CosmosRestProvider)
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<CosmosRestProvider> {
        return listOf(
            "https://rest-testnet.sei-apis.com",
            "https://sei-testnet-api.polkachu.com",
            "https://testnet-sei-api.lavenderfive.com",
            "https://rest-arctic-1.sei-apis.com",
            "https://rest.arctic-1.seinetwork.io/",
        ).map(::CosmosRestProvider)
    }
}