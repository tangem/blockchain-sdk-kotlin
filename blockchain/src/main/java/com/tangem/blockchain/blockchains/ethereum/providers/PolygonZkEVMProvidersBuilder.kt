package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.ProviderType

internal class PolygonZkEVMProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(
            "https://1rpc.io/polygon/zkevm/",
            "https://polygon-zkevm.drpc.org/",
            "https://polygon-zkevm-mainnet.public.blastapi.io/",
            "https://zkevm-rpc.com/",
            "https://polygon-zkevm.blockpi.network/v1/rpc/public/",
            "https://rpc.polygon-zkevm.gateway.fm/",
            "https://api.zan.top/node/v1/polygonzkevm/mainnet/public/",
        )
            .map(::EthereumJsonRpcProvider)
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf("https://rpc.cardona.zkevm-rpc.com/")
            .map(::EthereumJsonRpcProvider)
    }
}
