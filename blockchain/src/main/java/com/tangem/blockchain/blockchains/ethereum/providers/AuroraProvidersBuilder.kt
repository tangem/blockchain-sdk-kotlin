package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig

internal class AuroraProvidersBuilder(
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Aurora, Blockchain.AuroraTestnet)

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return if (blockchain.isTestnet()) {
            listOf(
                EthereumJsonRpcProvider(baseUrl = "https://testnet.aurora.dev/"),
            )
        } else {
            listOfNotNull(
                EthereumJsonRpcProvider(baseUrl = "https://mainnet.aurora.dev/"),
                EthereumJsonRpcProvider(baseUrl = "https://aurora.drpc.org/"),
            )
        }
    }
}
