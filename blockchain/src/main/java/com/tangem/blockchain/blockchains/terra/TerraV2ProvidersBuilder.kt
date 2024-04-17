package com.tangem.blockchain.blockchains.terra

import com.tangem.blockchain.blockchains.cosmos.network.CosmosRestProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank

internal class TerraV2ProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<CosmosRestProvider>() {

    override fun createProviders(blockchain: Blockchain): List<CosmosRestProvider> {
        return providerTypes
            .mapNotNull {
                when (it) {
                    is ProviderType.Public -> it.url
                    ProviderType.NowNodes -> createNowNodesProvider()
                    else -> null
                }
            }
            .map(::CosmosRestProvider)
    }

    private fun createNowNodesProvider(): String? {
        return config.nowNodeCredentials?.apiKey.letNotBlank { "https://luna.nownodes.io/$it/" }
    }
}