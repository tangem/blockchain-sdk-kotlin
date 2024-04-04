package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig

internal class EthereumProvidersBuilder(
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Ethereum, Blockchain.EthereumTestnet)

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return with(ethereumProviderFactory) {
            if (blockchain.isTestnet()) {
                listOfNotNull(
                    getNowNodesProvider(baseUrl = "https://eth-goerli.nownodes.io/"),
                    getInfuraProvider(baseUrl = "https://goerli.infura.io/v3/"),
                )
            } else {
                listOfNotNull(
                    getNowNodesProvider(baseUrl = "https://eth.nownodes.io/"),
                    getGetBlockProvider { eth?.jsonRpc },
                    getInfuraProvider(baseUrl = "https://mainnet.infura.io/v3/"),
                )
            }
        }
    }
}