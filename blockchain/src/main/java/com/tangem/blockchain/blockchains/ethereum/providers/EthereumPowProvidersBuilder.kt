package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig

internal class EthereumPowProvidersBuilder(
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.EthereumPow, Blockchain.EthereumPowTestnet)

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return if (blockchain.isTestnet()) {
            listOf(
                EthereumJsonRpcProvider(baseUrl = "https://iceberg.ethereumpow.org/"),
            )
        } else {
            listOfNotNull(
                ethereumProviderFactory.getNowNodesProvider(baseUrl = "https://ethw.nownodes.io/"),
                EthereumJsonRpcProvider(baseUrl = "https://mainnet.ethereumpow.org/"),
            )
        }
    }
}