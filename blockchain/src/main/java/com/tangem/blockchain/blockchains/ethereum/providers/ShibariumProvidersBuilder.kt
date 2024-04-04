package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig

internal class ShibariumProvidersBuilder(
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Shibarium, Blockchain.ShibariumTestnet)

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return if (blockchain.isTestnet()) {
            listOf(
                EthereumJsonRpcProvider(baseUrl = "https://puppynet.shibrpc.com/"),
            )
        } else {
            listOfNotNull(
                // the official api goes first due to the problems we have recently had with https://xdc.nownodes.io/
                EthereumJsonRpcProvider(baseUrl = "https://www.shibrpc.com/"),
                ethereumProviderFactory.getNowNodesProvider(baseUrl = "https://shib.nownodes.io/"),
            )
        }
    }
}