package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig

internal class MoonriverProvidersBuilder(
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Moonriver, Blockchain.MoonriverTestnet)

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return if (blockchain.isTestnet()) {
            listOf(
                "https://rpc.api.moonbase.moonbeam.network/",
            )
        } else {
            listOf(
                "https://moonriver.public.blastapi.io/",
                "https://moonriver-rpc.dwellir.com/",
                "https://moonriver-mainnet.gateway.pokt.network/v1/lb/62a74fdb123e6f003963642f/",
                "https://moonriver.unitedbloc.com/",
                "https://moonriver-rpc.publicnode.com/",
            )
        }
            .map(::EthereumJsonRpcProvider)
    }
}