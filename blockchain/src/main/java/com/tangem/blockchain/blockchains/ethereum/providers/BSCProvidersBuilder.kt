package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig

internal class BSCProvidersBuilder(
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        // https://docs.fantom.foundation/api/public-api-endpoints
        return listOfNotNull(
            EthereumJsonRpcProvider(baseUrl = "https://bsc-dataseed.binance.org/"),
            ethereumProviderFactory.getNowNodesProvider(baseUrl = "https://bsc.nownodes.io/"),
            ethereumProviderFactory.getGetBlockProvider { bsc?.jsonRpc },
            createQuickNodeProvider(),
        )
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(
            EthereumJsonRpcProvider(baseUrl = "https://data-seed-prebsc-1-s1.binance.org:8545/"),
        )
    }

    private fun createQuickNodeProvider(): EthereumJsonRpcProvider? {
        return config.quickNodeBscCredentials?.let { credentials ->
            if (credentials.subdomain.isNotBlank() && credentials.apiKey.isNotBlank()) {
                EthereumJsonRpcProvider(
                    "https://${credentials.subdomain}.bsc.discover.quiknode.pro/${credentials.apiKey}/",
                )
            } else {
                null
            }
        }
    }
}
