package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.ProviderType

internal class BlastProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return providerTypes.mapNotNull {
            when (it) {
                is ProviderType.Public -> EthereumJsonRpcProvider(it.url)
                ProviderType.NowNodes -> ethereumProviderFactory.getNowNodesProvider("https://blast.nownodes.io/")
                ProviderType.GetBlock -> ethereumProviderFactory.getGetBlockProvider { blast?.jsonRpc }
                else -> null
            }
        }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(
            "https://sepolia.blast.io/",
            "https://blast-sepolia.drpc.org",
            "https://blast-sepolia.blockpi.network/v1/rpc/public/",
        ).map(::EthereumJsonRpcProvider)
    }
}