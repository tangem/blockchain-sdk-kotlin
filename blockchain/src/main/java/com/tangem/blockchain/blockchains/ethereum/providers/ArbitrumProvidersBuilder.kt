package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig

internal class ArbitrumProvidersBuilder(
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOfNotNull(
            EthereumJsonRpcProvider(baseUrl = "https://arb1.arbitrum.io/rpc/"),
            ethereumProviderFactory.getNowNodesProvider(baseUrl = "https://arbitrum.nownodes.io/"),
            ethereumProviderFactory.getInfuraProvider(baseUrl = "https://arbitrum-mainnet.infura.io/v3/"),
        )
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(
            EthereumJsonRpcProvider(baseUrl = "https://goerli-rollup.arbitrum.io/rpc/"),
        )
    }
}