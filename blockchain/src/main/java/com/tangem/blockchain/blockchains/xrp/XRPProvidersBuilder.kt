package com.tangem.blockchain.blockchains.xrp

import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkProvider
import com.tangem.blockchain.blockchains.xrp.network.rippled.RippledNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.NowNodeCredentials
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank

internal class XRPProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<XrpNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<XrpNetworkProvider> {
        return providerTypes.mapNotNull { providerType ->
            when (providerType) {
                is ProviderType.Public -> RippledNetworkProvider(baseUrl = providerType.url)
                ProviderType.NowNodes -> createNowNodesProvider()
                ProviderType.GetBlock -> createGetBlockProvider()
                ProviderType.QuickNode -> createQuickNodeProvider()
                else -> null
            }
        }
    }

    private fun createNowNodesProvider(): XrpNetworkProvider? {
        return config.nowNodeCredentials?.apiKey.letNotBlank {
            RippledNetworkProvider(
                baseUrl = "https://xrp.nownodes.io/",
                apiKeyHeader = NowNodeCredentials.headerApiKey to it,
            )
        }
    }

    private fun createQuickNodeProvider(): XrpNetworkProvider? {
        return config.quickNodeXrpCredentials?.let { credentials ->
            if (credentials.subdomain.isNotBlank() && credentials.apiKey.isNotBlank()) {
                RippledNetworkProvider(baseUrl = "https://${credentials.subdomain}/${credentials.apiKey}/")
            } else {
                null
            }
        }
    }

    private fun createGetBlockProvider(): XrpNetworkProvider? {
        return config.getBlockCredentials?.xrp?.jsonRpc.letNotBlank { jsonRpcToken ->
            RippledNetworkProvider(baseUrl = "https://go.getblock.io/$jsonRpcToken/")
        }
    }
}