package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig

internal class AreonProvidersBuilder(
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Areon, Blockchain.AreonTestnet)

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return if (blockchain.isTestnet()) {
            listOf(
                "https://testnet-rpc.areon.network/",
                "https://testnet-rpc2.areon.network/",
                "https://testnet-rpc3.areon.network/",
                "https://testnet-rpc4.areon.network/",
                "https://testnet-rpc5.areon.network/",
            )
        } else {
            listOf(
                "https://mainnet-rpc.areon.network/",
                "https://mainnet-rpc2.areon.network/",
                "https://mainnet-rpc3.areon.network/",
                "https://mainnet-rpc4.areon.network/",
                "https://mainnet-rpc5.areon.network/",
            )
        }
            .map(::EthereumJsonRpcProvider)
    }
}