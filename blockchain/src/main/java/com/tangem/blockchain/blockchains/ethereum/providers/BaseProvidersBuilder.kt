package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig

internal class BaseProvidersBuilder(
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Base, Blockchain.BaseTestnet)

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return if (blockchain.isTestnet()) {
            listOf(
                "https://sepolia.base.org/",
                "https://rpc.notadegen.com/base/sepolia/",
                "https://base-sepolia-rpc.publicnode.com/",
            )
        } else {
            listOf(
                "https://mainnet.base.org/",
                "https://base.meowrpc.com/",
                "https://base-rpc.publicnode.com/",
                "https://base.drpc.org/",
                "https://base.llamarpc.com/",
            )
        }
            .map(::EthereumJsonRpcProvider)
    }
}