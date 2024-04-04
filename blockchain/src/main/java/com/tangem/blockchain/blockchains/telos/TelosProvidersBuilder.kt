package com.tangem.blockchain.blockchains.telos

import com.tangem.blockchain.blockchains.ethereum.EthereumLikeProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig

internal class TelosProvidersBuilder(
    override val config: BlockchainSdkConfig,
) : EthereumLikeProvidersBuilder(config) {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Telos, Blockchain.TelosTestnet)

    override fun createProviders(blockchain: Blockchain): List<EthereumJsonRpcProvider> {
        return if (blockchain.isTestnet()) {
            listOf(EthereumJsonRpcProvider(baseUrl = "https://telos-evm-testnet.rpc.thirdweb.com/"))
        } else {
            listOf(
                EthereumJsonRpcProvider(baseUrl = "https://mainnet.telos.net", postfixUrl = "evm"),
                EthereumJsonRpcProvider(baseUrl = "https://api.kainosbp.com", postfixUrl = "evm"),
                EthereumJsonRpcProvider(baseUrl = "https://telos-evm.rpc.thirdweb.com/"),
            )
        }
    }
}