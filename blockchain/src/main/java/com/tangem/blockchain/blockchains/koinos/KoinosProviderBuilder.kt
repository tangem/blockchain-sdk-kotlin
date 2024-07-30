package com.tangem.blockchain.blockchains.koinos

import com.tangem.blockchain.blockchains.koinos.network.KoinosNetworkProvier
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType

private const val KOINOS_PRO_URL = "https://api.koinos.pro/jsonrpc/"

internal class KoinosProviderBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<KoinosNetworkProvier>() {

    override fun createProviders(blockchain: Blockchain): List<KoinosNetworkProvier> {
        return providerTypes
            .mapNotNull {
                when (it) {
                    is ProviderType.Public -> createProvider(baseUrl = it.url, isTestnet = false)
                    is ProviderType.Koinos.KoinosPro -> createProvider(
                        baseUrl = KOINOS_PRO_URL,
                        isTestnet = false,
                        apiKey = config.koinosProApiKey ?: return@mapNotNull null,
                    )
                    else -> null
                }
            }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<KoinosNetworkProvier> {
        return listOf(
            createProvider(baseUrl = "https://harbinger-api.koinos.io/", isTestnet = true),
            createProvider(baseUrl = "https://testnet.koinosblocks.com/", isTestnet = true),
        )
    }

    private fun createProvider(baseUrl: String, isTestnet: Boolean, apiKey: String? = null): KoinosNetworkProvier {
        return KoinosNetworkProvier(baseUrl = baseUrl, isTestnet = isTestnet, apiKey = apiKey)
    }
}