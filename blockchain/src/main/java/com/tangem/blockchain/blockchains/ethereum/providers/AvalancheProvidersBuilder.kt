package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank

internal class AvalancheProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return providerTypes.mapNotNull {
            when (it) {
                is ProviderType.Public -> EthereumJsonRpcProvider(baseUrl = it.url)
                ProviderType.NowNodes -> createNowNodesProvider()
                ProviderType.GetBlock -> createGetBlockProvider()
                else -> null
            }
        }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(
            EthereumJsonRpcProvider(
                baseUrl = "https://api.avax-test.network/",
                postfixUrl = AVALANCHE_POSTFIX,
            ),
        )
    }

    private fun createNowNodesProvider(): EthereumJsonRpcProvider? {
        return config.nowNodeCredentials?.apiKey.letNotBlank {
            EthereumJsonRpcProvider(
                baseUrl = "https://avax.nownodes.io/",
                postfixUrl = AVALANCHE_POSTFIX,
                nowNodesApiKey = it, // special for Avalanche
            )
        }
    }

    private fun createGetBlockProvider(): EthereumJsonRpcProvider? {
        return config.getBlockCredentials?.avalanche?.jsonRpc.letNotBlank { avalancheToken ->
            EthereumJsonRpcProvider(
                baseUrl = "https://go.getblock.io/$avalancheToken/",
                postfixUrl = AVALANCHE_POSTFIX,
            )
        }
    }

    private companion object {
        const val AVALANCHE_POSTFIX = "ext/bc/C/rpc"
    }
}