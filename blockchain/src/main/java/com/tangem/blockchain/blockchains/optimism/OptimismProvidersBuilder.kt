package com.tangem.blockchain.blockchains.optimism

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig

internal class OptimismProvidersBuilder(
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Optimism, Blockchain.OptimismTestnet)

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return if (blockchain.isTestnet()) {
            listOf(
                EthereumJsonRpcProvider(baseUrl = "https://goerli.optimism.io/"),
            )
        } else {
            listOfNotNull(
                EthereumJsonRpcProvider(baseUrl = "https://mainnet.optimism.io/"),
                ethereumProviderFactory.getNowNodesProvider(baseUrl = "https://optimism.nownodes.io/"),
                EthereumJsonRpcProvider(baseUrl = "https://optimism-mainnet.public.blastapi.io/"),
                EthereumJsonRpcProvider(baseUrl = "https://rpc.ankr.com/optimism/"),
            )
        }
    }
}