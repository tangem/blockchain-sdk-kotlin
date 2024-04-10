package com.tangem.blockchain.blockchains.optimism

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.ProviderType

internal class OptimismProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOfNotNull(
            EthereumJsonRpcProvider(baseUrl = "https://mainnet.optimism.io/"),
            ethereumProviderFactory.getNowNodesProvider(baseUrl = "https://optimism.nownodes.io/"),
            EthereumJsonRpcProvider(baseUrl = "https://optimism-mainnet.public.blastapi.io/"),
            EthereumJsonRpcProvider(baseUrl = "https://rpc.ankr.com/optimism/"),
        )
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(
            EthereumJsonRpcProvider(baseUrl = "https://goerli.optimism.io/"),
        )
    }
}