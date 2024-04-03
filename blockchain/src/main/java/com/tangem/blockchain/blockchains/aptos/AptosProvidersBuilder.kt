package com.tangem.blockchain.blockchains.aptos

import com.tangem.blockchain.blockchains.aptos.network.AptosNetworkProvider
import com.tangem.blockchain.blockchains.aptos.network.provider.AptosRestNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.extensions.letNotBlank

internal class AptosProvidersBuilder(
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<AptosNetworkProvider>() {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Aptos, Blockchain.AptosTestnet)

    override fun createProviders(blockchain: Blockchain): List<AptosNetworkProvider> {
        return listOfNotNull(
            createOfficialNetworkProvider(blockchain),
            config.getBlockCredentials?.aptos?.rest?.letNotBlank(::createGetBlockNetworkProvider),
            config.nowNodeCredentials?.apiKey?.letNotBlank(::createNowNodesNetworkProvider),
        )
    }

    private fun createOfficialNetworkProvider(blockchain: Blockchain): AptosNetworkProvider {
        return AptosRestNetworkProvider(
            baseUrl = "https://fullnode.${if (blockchain.isTestnet()) "testnet" else "mainnet"}.aptoslabs.com/",
        )
    }

    private fun createNowNodesNetworkProvider(apiKey: String): AptosNetworkProvider {
        return AptosRestNetworkProvider(baseUrl = "https://apt.nownodes.io/$apiKey/")
    }

    private fun createGetBlockNetworkProvider(accessToken: String): AptosRestNetworkProvider {
        return AptosRestNetworkProvider(baseUrl = "https://go.getblock.io/$accessToken/")
    }
}