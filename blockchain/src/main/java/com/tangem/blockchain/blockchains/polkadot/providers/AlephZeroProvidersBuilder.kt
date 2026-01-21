package com.tangem.blockchain.blockchains.polkadot.providers

import com.tangem.blockchain.blockchains.polkadot.network.PolkadotCombinedProvider
import com.tangem.blockchain.blockchains.polkadot.network.PolkadotNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType

internal class AlephZeroProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<PolkadotNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<PolkadotNetworkProvider> {
        return providerTypes.mapNotNull {
            when (it) {
                is ProviderType.AlephZero.Dwellir -> createDwellirProvider(blockchain)
                is ProviderType.Public -> createPublicProvider(it.url, blockchain)
                else -> null
            }
        }
    }

    private fun createDwellirProvider(blockchain: Blockchain): PolkadotNetworkProvider? {
        return config.dwellirApiKey?.let {
            val baseUrl = if (blockchain.isTestnet()) {
                "https://api-aleph-zero-testnet.n.dwellir.com/$it/"
            } else {
                "https://api-aleph-zero-mainnet.n.dwellir.com/$it/"
            }
            PolkadotCombinedProvider(
                baseUrl = baseUrl,
                blockchain = blockchain,
                credentials = null,
            )
        }
    }

    private fun createPublicProvider(url: String, blockchain: Blockchain): PolkadotNetworkProvider {
        return PolkadotCombinedProvider(baseUrl = url, blockchain = blockchain)
    }
}