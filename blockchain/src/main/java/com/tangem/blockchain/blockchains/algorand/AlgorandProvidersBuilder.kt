package com.tangem.blockchain.blockchains.algorand

import com.tangem.blockchain.blockchains.algorand.network.AlgorandNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.extensions.letNotBlank

internal class AlgorandProvidersBuilder(
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<AlgorandNetworkProvider>() {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Algorand, Blockchain.AlgorandTestnet)

    override fun createProviders(blockchain: Blockchain): List<AlgorandNetworkProvider> {
        return buildList {
            if (!blockchain.isTestnet()) {
                config.nowNodeCredentials?.apiKey.letNotBlank(::createNowNodesProvider)?.let(::add)
                config.getBlockCredentials?.algorand?.rest.letNotBlank(::createGetBlockProvider)?.let(::add)
            }

            add(createAlgonodeProvider(blockchain))
        }
    }

    private fun createNowNodesProvider(apiKey: String): AlgorandNetworkProvider {
        return AlgorandNetworkProvider(baseUrl = "https://algo.nownodes.io/$apiKey/")
    }

    private fun createGetBlockProvider(apiKey: String): AlgorandNetworkProvider {
        return AlgorandNetworkProvider(baseUrl = "https://go.getblock.io/$apiKey/")
    }

    private fun createAlgonodeProvider(blockchain: Blockchain): AlgorandNetworkProvider {
        val network = if (blockchain.isTestnet()) "testnet" else "mainnet"
        return AlgorandNetworkProvider(baseUrl = "https://$network-api.algonode.cloud/")
    }
}