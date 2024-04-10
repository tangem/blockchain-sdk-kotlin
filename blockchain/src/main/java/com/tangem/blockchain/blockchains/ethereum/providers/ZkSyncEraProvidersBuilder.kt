package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig

internal class ZkSyncEraProvidersBuilder(
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(
            "https://mainnet.era.zksync.io/",
            "https://zksync-era.blockpi.network/v1/rpc/public/",
            "https://1rpc.io/zksync2-era/",
            "https://zksync.meowrpc.com/",
            "https://zksync.drpc.org/",
        )
            .map(::EthereumJsonRpcProvider)
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(
            "https://sepolia.era.zksync.dev/",
            "https://zksync-era-sepolia.blockpi.network/v1/rpc/public/",
        )
            .map(::EthereumJsonRpcProvider)
    }
}
