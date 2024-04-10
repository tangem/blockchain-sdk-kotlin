package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig

internal class EthereumProvidersBuilder(
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOfNotNull(
            ethereumProviderFactory.getNowNodesProvider(baseUrl = "https://eth.nownodes.io/"),
            ethereumProviderFactory.getGetBlockProvider { eth?.jsonRpc },
            ethereumProviderFactory.getInfuraProvider(baseUrl = "https://mainnet.infura.io/v3/"),
        )
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOfNotNull(
            ethereumProviderFactory.getNowNodesProvider(baseUrl = "https://eth-goerli.nownodes.io/"),
            ethereumProviderFactory.getInfuraProvider(baseUrl = "https://goerli.infura.io/v3/"),
        )
    }
}