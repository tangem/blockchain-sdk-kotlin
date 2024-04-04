package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig

internal class PolygonProvidersBuilder(
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Polygon, Blockchain.PolygonTestnet)

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        // https://wiki.polygon.technology/docs/operate/network-rpc-endpoints
        return if (blockchain.isTestnet()) {
            listOf(
                EthereumJsonRpcProvider(baseUrl = "https://rpc-mumbai.maticvigil.com/"),
            )
        } else {
            listOfNotNull(
                EthereumJsonRpcProvider(baseUrl = "https://polygon-rpc.com/"),
                ethereumProviderFactory.getNowNodesProvider(baseUrl = "https://matic.nownodes.io/"),
                ethereumProviderFactory.getGetBlockProvider { polygon?.jsonRpc },
                EthereumJsonRpcProvider(baseUrl = "https://rpc-mainnet.maticvigil.com/"),
                EthereumJsonRpcProvider(baseUrl = "https://rpc-mainnet.matic.quiknode.pro/"),
            )
        }
    }
}