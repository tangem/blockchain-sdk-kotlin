package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.createWithPostfixIfContained
import com.tangem.blockchain.common.network.providers.ProviderType

internal class ArbitrumProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return providerTypes.mapNotNull {
            when (it) {
                // Remove temporarily because of yield supply and incorrect pending tx count
                // is ProviderType.Blink -> ethereumProviderFactory.getBlinkProvider("https://arb.blinklabs.xyz/v1/")
                is ProviderType.Public -> createPublicProvider(url = it.url)
                ProviderType.NowNodes -> {
                    ethereumProviderFactory.getNowNodesProvider(baseUrl = "https://arbitrum.nownodes.io/")
                }
                ProviderType.EthereumLike.Infura -> {
                    ethereumProviderFactory.getInfuraProvider(baseUrl = "https://arbitrum-mainnet.infura.io/v3/")
                }
                ProviderType.GetBlock -> {
                    ethereumProviderFactory.getGetBlockProvider { arbitrum?.jsonRpc }
                }
                else -> null
            }
        }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(
            EthereumJsonRpcProvider(baseUrl = "https://goerli-rollup.arbitrum.io/rpc/"),
        )
    }

    private fun createPublicProvider(url: String): EthereumJsonRpcProvider {
        return createWithPostfixIfContained(
            baseUrl = url,
            postfixUrl = POSTFIX_URL,
            create = ::EthereumJsonRpcProvider,
        )
    }

    private companion object {
        const val POSTFIX_URL = "arb"
    }
}