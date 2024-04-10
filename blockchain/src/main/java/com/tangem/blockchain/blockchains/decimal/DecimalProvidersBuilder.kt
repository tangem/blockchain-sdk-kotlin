package com.tangem.blockchain.blockchains.decimal

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig

internal class DecimalProvidersBuilder(
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf(
            "https://node.decimalchain.com/web3/",
            "https://node1-mainnet.decimalchain.com/web3/",
            "https://node2-mainnet.decimalchain.com/web3/",
            "https://node3-mainnet.decimalchain.com/web3/",
            "https://node4-mainnet.decimalchain.com/web3/",
        )
            .map(::EthereumJsonRpcProvider)
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return listOf("https://testnet-val.decimalchain.com/web3/")
            .map(::EthereumJsonRpcProvider)
    }
}
