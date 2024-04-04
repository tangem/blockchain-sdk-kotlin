package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig

internal class PolygonZkEVMProvidersBuilder(
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override val supportedBlockchains: List<Blockchain> =
        listOf(Blockchain.PolygonZkEVM, Blockchain.PolygonZkEVMTestnet)

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return if (blockchain.isTestnet()) {
            listOf(
                "https://rpc.cardona.zkevm-rpc.com/",
            )
        } else {
            listOf(
                "https://1rpc.io/polygon/zkevm/",
                "https://polygon-zkevm.drpc.org/",
                "https://polygon-zkevm-mainnet.public.blastapi.io/",
                "https://zkevm-rpc.com/",
                "https://polygon-zkevm.blockpi.network/v1/rpc/public/",
                "https://rpc.polygon-zkevm.gateway.fm/",
                "https://api.zan.top/node/v1/polygonzkevm/mainnet/public/",
            )
        }
            .map(::EthereumJsonRpcProvider)
    }
}