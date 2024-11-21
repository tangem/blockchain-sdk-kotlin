package com.tangem.blockchain.blockchains.alephium

import com.tangem.blockchain.blockchains.alephium.network.AlephiumNetworkProvider
import com.tangem.blockchain.blockchains.alephium.network.AlephiumRestNetworkService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank

internal class AlephiumProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<AlephiumNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<AlephiumNetworkProvider> {
        return providerTypes.mapNotNull {
            when (it) {
                is ProviderType.Public -> AlephiumRestNetworkService(baseUrl = it.url)
                ProviderType.NowNodes -> createNowNodesNetworkProvider()
                else -> null
            }
        }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<AlephiumNetworkProvider> {
        return listOf(
            AlephiumRestNetworkService(baseUrl = "https://node.testnet.alephium.org/"),
        )
    }

    private fun createNowNodesNetworkProvider(): AlephiumNetworkProvider? {
        return config.nowNodeCredentials?.apiKey?.letNotBlank {
            AlephiumRestNetworkService(baseUrl = "https://alephium.nownodes.io/$it/")
        }
    }
}