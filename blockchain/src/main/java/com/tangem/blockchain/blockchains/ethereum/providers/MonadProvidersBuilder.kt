package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.ProviderType

internal class MonadProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return providerTypes.mapNotNull { type ->
            when (type) {
                is ProviderType.Public -> EthereumJsonRpcProvider(type.url)
                ProviderType.QuickNode -> createQuickNodeProvider()
                ProviderType.GetBlock -> ethereumProviderFactory.getGetBlockProvider { monad?.rest }
                else -> null
            }
        }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(
            EthereumJsonRpcProvider("https://testnet-rpc.monad.xyz/"),
        )
    }

    private fun createQuickNodeProvider(): EthereumJsonRpcProvider? {
        return config.quickNodeMonadCredentials?.let { credentials ->
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