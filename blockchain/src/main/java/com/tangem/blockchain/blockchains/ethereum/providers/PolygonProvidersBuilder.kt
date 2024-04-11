package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.ProviderType

internal class PolygonProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        // https://wiki.polygon.technology/docs/operate/network-rpc-endpoints
        return listOfNotNull(
            EthereumJsonRpcProvider(baseUrl = "https://polygon-rpc.com/"),
            ethereumProviderFactory.getNowNodesProvider(baseUrl = "https://matic.nownodes.io/"),
            ethereumProviderFactory.getGetBlockProvider { polygon?.jsonRpc },
            EthereumJsonRpcProvider(baseUrl = "https://rpc-mainnet.maticvigil.com/"),
            EthereumJsonRpcProvider(baseUrl = "https://rpc-mainnet.matic.quiknode.pro/"),
        )
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(
            EthereumJsonRpcProvider(baseUrl = "https://rpc-mumbai.maticvigil.com/"),
        )
    }
}