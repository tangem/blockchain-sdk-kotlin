package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.ProviderType

internal class FantomProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOfNotNull(
            ethereumProviderFactory.getNowNodesProvider(baseUrl = "https://ftm.nownodes.io/"),
            ethereumProviderFactory.getGetBlockProvider { fantom?.jsonRpc },
            EthereumJsonRpcProvider(baseUrl = "https://rpc.ftm.tools/"),
            EthereumJsonRpcProvider(baseUrl = "https://rpcapi.fantom.network/"),
            EthereumJsonRpcProvider(baseUrl = "https://fantom-mainnet.public.blastapi.io/"),
            EthereumJsonRpcProvider(baseUrl = "https://rpc.ankr.com/", postfixUrl = "fantom"),
        )
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(
            EthereumJsonRpcProvider(baseUrl = "https://rpc.testnet.fantom.network/"),
        )
    }
}