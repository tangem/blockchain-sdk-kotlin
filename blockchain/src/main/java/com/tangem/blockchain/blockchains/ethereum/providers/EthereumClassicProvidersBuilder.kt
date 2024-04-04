package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig

internal class EthereumClassicProvidersBuilder(
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override val supportedBlockchains: List<Blockchain> =
        listOf(Blockchain.EthereumClassic, Blockchain.EthereumClassicTestnet)

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return if (blockchain.isTestnet()) {
            listOf(
                EthereumJsonRpcProvider(baseUrl = "https://etc.rivet.link/kotti/"),
            )
        } else {
            listOfNotNull(
                EthereumJsonRpcProvider(baseUrl = "https://etc.etcdesktop.com/"),
                ethereumProviderFactory.getGetBlockProvider { etc?.jsonRpc },
                EthereumJsonRpcProvider(baseUrl = "https://etc.rivet.link/etc/"),
                EthereumJsonRpcProvider(baseUrl = "https://etc.mytokenpocket.vip/"),
                EthereumJsonRpcProvider(baseUrl = "https://besu-de.etc-network.info/"),
                EthereumJsonRpcProvider(baseUrl = "https://geth-at.etc-network.info/"),
            )
        }
    }
}