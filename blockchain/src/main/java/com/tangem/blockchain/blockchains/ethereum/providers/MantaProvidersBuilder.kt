package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig

internal class MantaProvidersBuilder(
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Manta, Blockchain.MantaTestnet)

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return if (blockchain.isTestnet()) {
            listOf(
                "https://pacific-rpc.testnet.manta.network/http/",
            )
        } else {
            listOf(
                "https://manta-pacific.drpc.org/",
                "https://pacific-rpc.manta.network/http/",
                "https://1rpc.io/manta/",
            )
        }
            .map(::EthereumJsonRpcProvider)
    }
}