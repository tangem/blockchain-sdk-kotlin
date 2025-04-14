package com.tangem.blockchain.blockchains.ravencoin

import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.blockchains.ravencoin.network.RavencoinNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.network.blockbook.BlockBookNetworkProviderFactory

internal class RavencoinProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<BitcoinNetworkProvider>() {

    private val blockBookNetworkProviderFactory by lazy { BlockBookNetworkProviderFactory(config) }

    override fun createProviders(blockchain: Blockchain): List<BitcoinNetworkProvider> {
        return providerTypes.mapNotNull {
            when (it) {
                is ProviderType.Public -> RavencoinNetworkProvider(baseUrl = it.url)
                ProviderType.NowNodes -> blockBookNetworkProviderFactory.createNowNodesProvider(blockchain)
                else -> null
            }
        }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<BitcoinNetworkProvider> {
        return listOf(RavencoinNetworkProvider("https://testnet.ravencoin.network/api/"))
    }
}