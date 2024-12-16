package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.createWithPostfixIfContained
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank

internal class FlareProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return providerTypes.mapNotNull {
            when (it) {
                is ProviderType.Public -> createPublickProvider(url = it.url)
                ProviderType.NowNodes -> config.nowNodeCredentials?.apiKey.letNotBlank { nowNodesApiKey ->
                    EthereumJsonRpcProvider(baseUrl = "https://flr.nownodes.io/$nowNodesApiKey/ext/bc/C/rpc")
                }
                else -> null
            }
        }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(createPublickProvider("https://coston2-api.flare.network/ext/C/rpc/"))
    }

    private fun createPublickProvider(url: String): EthereumJsonRpcProvider {
        return createWithPostfixIfContained(
            baseUrl = url,
            postfixUrl = POSTFIX_URLS.toTypedArray(),
            create = ::EthereumJsonRpcProvider,
        )
    }

    private companion object {
        val POSTFIX_URLS = listOf("ext/C/rpc", "ext/bc/C/rpc")
    }
}