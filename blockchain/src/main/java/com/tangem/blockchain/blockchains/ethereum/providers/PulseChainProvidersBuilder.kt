package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig

internal class PulseChainProvidersBuilder(
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.PulseChain, Blockchain.PulseChainTestnet)

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return if (blockchain.isTestnet()) {
            listOf(
                "https://rpc.v4.testnet.pulsechain.com/",
                "https://pulsechain-testnet.publicnode.com/",
                "https://rpc-testnet-pulsechain.g4mm4.io/",
            )
        } else {
            listOf(
                "https://rpc.pulsechain.com/",
                "https://pulsechain.publicnode.com/",
                "https://rpc-pulsechain.g4mm4.io/",
            )
        }
            .map(::EthereumJsonRpcProvider)
    }
}