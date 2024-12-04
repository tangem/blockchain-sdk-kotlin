package com.tangem.blockchain.blockchains.clore

import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.network.blockbook.BlockBookNetworkProviderFactory

internal class CloreProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<BitcoinNetworkProvider>() {

    private val blockBookProviderFactory by lazy { BlockBookNetworkProviderFactory(config) }

    override fun createProviders(blockchain: Blockchain): List<BitcoinNetworkProvider> {
        return providerTypes.mapNotNull {
            when (it) {
                is ProviderType.Public ->
                    blockBookProviderFactory
                        .createCloreBlockProvider(blockchain = blockchain, baseHost = it.url)
                else -> null
            }
        }
    }
}