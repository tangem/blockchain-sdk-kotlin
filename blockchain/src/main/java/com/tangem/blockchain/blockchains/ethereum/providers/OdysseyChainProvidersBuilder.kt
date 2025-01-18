package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.createWithPostfixIfContained
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank

internal class OdysseyChainProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(createPublicProvider("https://testnode.dioneprotocol.com/ext/bc/D/rpc"))
    }

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return providerTypes.mapNotNull {
            when (it) {
                is ProviderType.Public -> createPublicProvider(url = it.url)
                ProviderType.NowNodes -> config.nowNodeCredentials?.apiKey.letNotBlank { nowNodesApiKey ->
                    EthereumJsonRpcProvider(
                        baseUrl = "https://odyssey.nownodes.io/",
                        postfixUrl = POSTFIX_URL,
                        nowNodesApiKey = nowNodesApiKey,
                    )
                }
                else -> null
            }
        }
    }

    private fun createPublicProvider(url: String): EthereumJsonRpcProvider {
        return createWithPostfixIfContained(
            baseUrl = url,
            postfixUrl = POSTFIX_URL,
            create = ::EthereumJsonRpcProvider,
        )
    }

    private companion object {
        const val POSTFIX_URL = "ext/bc/D/rpc"
    }
}