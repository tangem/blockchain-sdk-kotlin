package com.tangem.blockchain.blockchains.stellar

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank
import org.stellar.sdk.Server

internal class StellarProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<StellarWrapperNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<StellarWrapperNetworkProvider> {
        return providerTypes
            .mapNotNull { providerType ->
                when (providerType) {
                    is ProviderType.Public -> StellarNetwork.Public(providerType.url)
                    ProviderType.NowNodes -> config.nowNodeCredentials?.apiKey.letNotBlank(StellarNetwork::Nownodes)
                    ProviderType.QuickNode -> createQuickNodeNetwork()
                    ProviderType.GetBlock -> createGetBlockNetwork()
                    else -> null
                }
            }
            .map(::createWrapperProvider)
    }

    private fun createQuickNodeNetwork(): StellarNetwork? {
        return config.quickNodeStellarCredentials?.let { credentials ->
            if (credentials.subdomain.isNotBlank() && credentials.apiKey.isNotBlank()) {
                StellarNetwork.QuickNode(credentials.subdomain, credentials.apiKey)
            } else {
                null
            }
        }
    }

    private fun createGetBlockNetwork(): StellarNetwork? {
        return config.getBlockCredentials?.stellar?.rest.letNotBlank(StellarNetwork::GetBlock)
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<StellarWrapperNetworkProvider> {
        return listOf(
            createWrapperProvider(StellarNetwork.HorizonTestnet),
        )
    }

    private fun createWrapperProvider(network: StellarNetwork): StellarWrapperNetworkProvider {
        return StellarWrapperNetworkProvider(server = Server(network.url), url = network.url)
    }
}