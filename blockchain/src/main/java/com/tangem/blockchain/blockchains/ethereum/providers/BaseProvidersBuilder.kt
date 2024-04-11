package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.ProviderType

internal class BaseProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(
            "https://mainnet.base.org/",
            "https://base.meowrpc.com/",
            "https://base-rpc.publicnode.com/",
            "https://base.drpc.org/",
            "https://base.llamarpc.com/",
        )
            .map(::EthereumJsonRpcProvider)
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(
            "https://sepolia.base.org/",
            "https://rpc.notadegen.com/base/sepolia/",
            "https://base-sepolia-rpc.publicnode.com/",
        )
            .map(::EthereumJsonRpcProvider)
    }
}