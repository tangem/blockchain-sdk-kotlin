package com.tangem.blockchain.blockchains.adi

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.ProviderType

internal class AdiProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return providerTypes.mapNotNull { type ->
            when (type) {
                is ProviderType.Public -> EthereumJsonRpcProvider(type.url)
                ProviderType.Alchemy -> createAlchemyProvider(blockchain)
                else -> null
            }
        }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOfNotNull(
            EthereumJsonRpcProvider("https://rpc.ab.testnet.adifoundation.ai"),
            createAlchemyProvider(blockchain),
        )
    }

    private fun createAlchemyProvider(blockchain: Blockchain): EthereumJsonRpcProvider? {
        val apiKey = config.alchemyApiKey?.takeIf(String::isNotBlank) ?: return null
        val subdomain = if (blockchain.isTestnet()) "adi-testnet" else "adi-mainnet"
        return EthereumJsonRpcProvider(baseUrl = "https://$subdomain.g.alchemy.com/v2/$apiKey/")
    }
}