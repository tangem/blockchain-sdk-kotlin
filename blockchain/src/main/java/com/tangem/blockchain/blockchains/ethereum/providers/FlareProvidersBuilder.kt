package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig

internal class FlareProvidersBuilder(
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Flare, Blockchain.FlareTestnet)

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return if (blockchain.isTestnet()) {
            listOf(
                "https://coston2-api.flare.network/ext/C/rpc/",
            )
        } else {
            listOf(
                "https://flare-api.flare.network/ext/bc/C/rpc/",
                "https://flare.rpc.thirdweb.com/",
                "https://flare-bundler.etherspot.io/",
                "https://rpc.ankr.com/flare/",
                "https://flare.solidifi.app/ext/C/rpc/",
            )
        }
            .map(::EthereumJsonRpcProvider)
    }
}