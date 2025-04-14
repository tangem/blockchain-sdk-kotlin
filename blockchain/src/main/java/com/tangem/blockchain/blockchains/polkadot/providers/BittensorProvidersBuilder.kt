package com.tangem.blockchain.blockchains.polkadot.providers

import com.tangem.blockchain.blockchains.polkadot.network.PolkadotCombinedProvider
import com.tangem.blockchain.blockchains.polkadot.network.PolkadotNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType

internal class BittensorProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<PolkadotNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<PolkadotNetworkProvider> {
        return providerTypes.mapNotNull {
            when (it) {
                is ProviderType.Bittensor.Dwellir -> createDwellirProvider(blockchain)
                is ProviderType.Bittensor.Onfinality -> createOnfinalityProvider(blockchain)
                is ProviderType.Public -> createPublicProvider(it.url, blockchain)
                else -> null
            }
        }
    }

    private fun createDwellirProvider(blockchain: Blockchain): PolkadotNetworkProvider? {
        return config.bittensorDwellirApiKey?.let {
            PolkadotCombinedProvider(
                baseUrl = "https://api-bittensor-mainnet.dwellir.com/$it/",
                blockchain = blockchain,
                credentials = null,
            )
        }
    }

    private fun createOnfinalityProvider(blockchain: Blockchain): PolkadotNetworkProvider? {
        return config.bittensorOnfinalityApiKey?.let {
            PolkadotCombinedProvider(
                baseUrl = "https://bittensor-finney.api.onfinality.io/rpc/",
                blockchain = blockchain,
                credentials = mapOf("apikey" to it),
            )
        }
    }

    private fun createPublicProvider(url: String, blockchain: Blockchain): PolkadotNetworkProvider {
        return PolkadotCombinedProvider(baseUrl = url, blockchain = blockchain)
    }
}