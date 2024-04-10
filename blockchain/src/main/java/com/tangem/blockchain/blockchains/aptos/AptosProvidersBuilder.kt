package com.tangem.blockchain.blockchains.aptos

import com.tangem.blockchain.blockchains.aptos.network.AptosNetworkProvider
import com.tangem.blockchain.blockchains.aptos.network.provider.AptosRestNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank

internal class AptosProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<AptosNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<AptosNetworkProvider> {
        return listOfNotNull(
            createOfficialNetworkProvider(isTestnet = false),
            createGetBlockNetworkProvider(),
            createNowNodesNetworkProvider(),
        )
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<AptosNetworkProvider> {
        return listOf(createOfficialNetworkProvider(isTestnet = true))
    }

    private fun createOfficialNetworkProvider(isTestnet: Boolean): AptosNetworkProvider {
        return AptosRestNetworkProvider(
            baseUrl = "https://fullnode.${if (isTestnet) "testnet" else "mainnet"}.aptoslabs.com/",
        )
    }

    private fun createGetBlockNetworkProvider(): AptosRestNetworkProvider? {
        return config.getBlockCredentials?.aptos?.rest?.letNotBlank {
            AptosRestNetworkProvider(baseUrl = "https://go.getblock.io/$it/")
        }
    }

    private fun createNowNodesNetworkProvider(): AptosNetworkProvider? {
        return config.nowNodeCredentials?.apiKey?.letNotBlank {
            AptosRestNetworkProvider(baseUrl = "https://apt.nownodes.io/$it/")
        }
    }
}