package com.tangem.blockchain.blockchains.gonka

import com.tangem.blockchain.blockchains.cosmos.network.CosmosRestProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType

/**
 * Gonka uses the standard Cosmos REST layout. Hosts (e.g. https://rpc.gonka.gg/chain-api/) are
 * supplied as public providers via the app config (providers_order.json), not hardcoded here.
 * Mainnet only — there is no Gonka testnet.
 */
internal class GonkaProvidersBuilder(
    override val providerTypes: List<ProviderType>,
) : NetworkProvidersBuilder<CosmosRestProvider>() {

    override fun createProviders(blockchain: Blockchain): List<CosmosRestProvider> {
        return providerTypes.mapNotNull { providerType ->
            when (providerType) {
                is ProviderType.Public -> providerType.url
                else -> null
            }
        }
            .map(::CosmosRestProvider)
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<CosmosRestProvider> = emptyList()
}