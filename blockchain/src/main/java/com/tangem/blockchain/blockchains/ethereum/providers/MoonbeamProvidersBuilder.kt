package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig

internal class MoonbeamProvidersBuilder(
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(
            "https://rpc.api.moonbeam.network/",
            "https://1rpc.io/glmr/",
            "https://moonbeam.public.blastapi.io/",
            "https://moonbeam-rpc.dwellir.com/",
            "https://moonbeam-mainnet.gateway.pokt.network/v1/lb/629a2b5650ec8c0039bb30f0/",
            "https://moonbeam.unitedbloc.com/",
            "https://moonbeam-rpc.publicnode.com/",
            "https://rpc.ankr.com/moonbeam/",
        )
            .map(::EthereumJsonRpcProvider)
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(
            "https://moonbase-alpha.public.blastapi.io/",
            "https://moonbase-rpc.dwellir.com/",
            "https://rpc.api.moonbase.moonbeam.network/",
            "https://moonbase.unitedbloc.com/",
            "https://moonbeam-alpha.api.onfinality.io/public/",
        )
            .map(::EthereumJsonRpcProvider)
    }
}