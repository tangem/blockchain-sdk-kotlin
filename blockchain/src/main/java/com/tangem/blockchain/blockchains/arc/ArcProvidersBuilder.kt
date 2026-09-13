package com.tangem.blockchain.blockchains.arc

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.ProviderType

internal class ArcProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return providerTypes.mapNotNull { type ->
            when (type) {
                is ProviderType.Public -> EthereumJsonRpcProvider(baseUrl = type.url)
                ProviderType.QuickNode -> createQuickNodeProvider()
                ProviderType.Alchemy -> createAlchemyProvider(blockchain)
                else -> null
            }
        }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOfNotNull(
            EthereumJsonRpcProvider(baseUrl = TESTNET_URL),
            createAlchemyProvider(blockchain),
        )
    }

    private fun createQuickNodeProvider(): EthereumJsonRpcProvider? {
        val credentials = config.quickNodeArcCredentials ?: return null
        if (credentials.subdomain.isBlank() || credentials.apiKey.isBlank()) return null

        return EthereumJsonRpcProvider(baseUrl = "https://${credentials.subdomain}/${credentials.apiKey}/")
    }

    private fun createAlchemyProvider(blockchain: Blockchain): EthereumJsonRpcProvider? {
        val apiKey = config.alchemyApiKey?.takeIf(String::isNotBlank) ?: return null
        val subdomain = if (blockchain.isTestnet()) "arc-testnet" else "arc-mainnet"

        return EthereumJsonRpcProvider(baseUrl = "https://$subdomain.g.alchemy.com/v2/$apiKey/")
    }

    private companion object {
        const val TESTNET_URL = "https://rpc.testnet.arc.io/"
    }
}