package com.tangem.blockchain.blockchains.cosmos

import com.tangem.blockchain.blockchains.cosmos.network.CosmosRestProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank

internal class CosmosProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<CosmosRestProvider>() {

    override fun createProviders(blockchain: Blockchain): List<CosmosRestProvider> {
        return providerTypes.mapNotNull {
            when (it) {
                is ProviderType.Public -> it.url
                ProviderType.NowNodes -> createNowNodesProvider()
                ProviderType.GetBlock -> createGetBlockProvider()
                else -> null
            }
        }
            .map(::CosmosRestProvider)
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<CosmosRestProvider> {
        return listOf(
            CosmosRestProvider(baseUrl = "https://rest.seed-01.theta-testnet.polypore.xyz"),
        )
    }

    private fun createNowNodesProvider(): String? {
        return config.nowNodeCredentials?.apiKey.letNotBlank { "https://atom.nownodes.io/$it/" }
    }

    private fun createGetBlockProvider(): String? {
        return config.getBlockCredentials?.cosmos?.rest.letNotBlank { "https://go.getblock.io/$it/" }
    }
}