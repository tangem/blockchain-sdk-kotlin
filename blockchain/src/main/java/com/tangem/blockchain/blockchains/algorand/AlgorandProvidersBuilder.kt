package com.tangem.blockchain.blockchains.algorand

import com.tangem.blockchain.blockchains.algorand.network.AlgorandNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank

internal class AlgorandProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<AlgorandNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<AlgorandNetworkProvider> {
        return listOfNotNull(
            createNowNodesProvider(),
            createGetBlockProvider(),
            createAlgonodeProvider(isTestnet = false),
        )
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<AlgorandNetworkProvider> {
        return listOf(createAlgonodeProvider(isTestnet = true))
    }

    private fun createNowNodesProvider(): AlgorandNetworkProvider? {
        return config.nowNodeCredentials?.apiKey.letNotBlank {
            AlgorandNetworkProvider(baseUrl = "https://algo.nownodes.io/$it/")
        }
    }

    private fun createGetBlockProvider(): AlgorandNetworkProvider? {
        return config.getBlockCredentials?.algorand?.rest.letNotBlank {
            AlgorandNetworkProvider(baseUrl = "https://go.getblock.io/$it/")
        }
    }

    private fun createAlgonodeProvider(isTestnet: Boolean): AlgorandNetworkProvider {
        val network = if (isTestnet) "testnet" else "mainnet"
        return AlgorandNetworkProvider(baseUrl = "https://$network-api.algonode.cloud/")
    }
}