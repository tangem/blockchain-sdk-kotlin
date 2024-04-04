package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig

internal class MantleProvidersBuilder(
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Mantle, Blockchain.MantleTestnet)

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return if (blockchain.isTestnet()) {
            listOf(
                "https://rpc.testnet.mantle.xyz/",
            )
        } else {
            listOf(
                "https://rpc.mantle.xyz/",
                "https://mantle-rpc.publicnode.com/",
                "https://mantle-mainnet.public.blastapi.io/",
                "https://rpc.ankr.com/mantle/",
                "https://1rpc.io/mantle/",
            )
        }
            .map(::EthereumJsonRpcProvider)
    }
}