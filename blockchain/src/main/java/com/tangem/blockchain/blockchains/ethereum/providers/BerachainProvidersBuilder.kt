package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.ProviderType

internal class BerachainProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return providerTypes.mapNotNull { providerType ->
            when (providerType) {
                is ProviderType.Public -> EthereumJsonRpcProvider(baseUrl = providerType.url)
                ProviderType.GetBlock -> ethereumProviderFactory.getGetBlockProvider { berachain?.jsonRpc }
                ProviderType.NowNodes -> ethereumProviderFactory.getNowNodesProvider("https://bera.nownodes.io/")
                ProviderType.QuickNode -> createQuickNodeProvider()
                else -> null
            }
        }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(
            EthereumJsonRpcProvider(baseUrl = "https://bepolia.rpc.berachain.com/"),
        )
    }

    private fun createQuickNodeProvider(): EthereumJsonRpcProvider? {
        return config.quickNodeBerachainCredentials?.let { credentials ->
            if (credentials.subdomain.isNotBlank() && credentials.apiKey.isNotBlank()) {
                EthereumJsonRpcProvider(
                    "https://${credentials.subdomain}/${credentials.apiKey}/",
                )
            } else {
                null
            }
        }
    }
}