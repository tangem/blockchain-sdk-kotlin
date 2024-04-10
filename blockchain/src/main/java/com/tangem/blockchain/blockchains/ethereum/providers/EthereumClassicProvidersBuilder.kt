package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.ProviderType

internal class EthereumClassicProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOfNotNull(
            EthereumJsonRpcProvider(baseUrl = "https://etc.etcdesktop.com/"),
            ethereumProviderFactory.getGetBlockProvider { etc?.jsonRpc },
            EthereumJsonRpcProvider(baseUrl = "https://etc.rivet.link/etc/"),
            EthereumJsonRpcProvider(baseUrl = "https://etc.mytokenpocket.vip/"),
            EthereumJsonRpcProvider(baseUrl = "https://besu-de.etc-network.info/"),
            EthereumJsonRpcProvider(baseUrl = "https://geth-at.etc-network.info/"),
        )
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(
            EthereumJsonRpcProvider(baseUrl = "https://etc.rivet.link/kotti/"),
        )
    }
}