package com.tangem.blockchain.blockchains.kaspa

import com.tangem.blockchain.blockchains.kaspa.network.KaspaNetworkProvider
import com.tangem.blockchain.blockchains.kaspa.network.KaspaRestApiNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.extensions.letNotBlank

internal class KaspaProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<KaspaNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<KaspaNetworkProvider> {
        return providerTypes
            .mapNotNull { type ->
                when (type) {
                    is ProviderType.Public -> type.url
                    ProviderType.Kaspa.SecondaryAPI -> config.kaspaSecondaryApiUrl?.letNotBlank { it }
                    else -> null
                }
            }
            .map(::KaspaRestApiNetworkProvider)
    }
}