package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.ProviderType

internal class EthereumProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return providerTypes.mapNotNull {
            when (it) {
                is ProviderType.Public -> {
                    ethereumProviderFactory.getPublicProvider(baseUrl = it.url)
                }
                ProviderType.NowNodes -> ethereumProviderFactory.getNowNodesProvider(
                    baseUrl = "https://eth.nownodes.io/",
                )
                ProviderType.GetBlock -> ethereumProviderFactory.getGetBlockProvider { eth?.jsonRpc }
                ProviderType.EthereumLike.Infura -> {
                    ethereumProviderFactory.getInfuraProvider(baseUrl = "https://mainnet.infura.io/v3/")
                }
                else -> null
            }
        }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOfNotNull(
            ethereumProviderFactory.getNowNodesProvider(baseUrl = "https://eth-sepolia.nownodes.io/"),
            ethereumProviderFactory.getInfuraProvider(baseUrl = "https://sepolia.infura.io/v3/"),
        )
    }
}