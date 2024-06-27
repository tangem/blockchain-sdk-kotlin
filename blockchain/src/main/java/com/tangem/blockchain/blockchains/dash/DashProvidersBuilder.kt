package com.tangem.blockchain.blockchains.dash

import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.network.blockbook.BlockBookNetworkProviderFactory
import com.tangem.blockchain.network.blockchair.BlockchairNetworkProviderFactory
import com.tangem.blockchain.network.blockcypher.BlockcypherNetworkProviderFactory

internal class DashProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<BitcoinNetworkProvider>() {

    private val blockBookNetworkProviderFactory by lazy { BlockBookNetworkProviderFactory(config) }
    private val blockchairNetworkProviderFactory by lazy { BlockchairNetworkProviderFactory(config) }
    private val blockcypherNetworkProviderFactory by lazy { BlockcypherNetworkProviderFactory(config) }

    override fun createProviders(blockchain: Blockchain): List<BitcoinNetworkProvider> {
        return providerTypes.flatMap {
            when (it) {
                ProviderType.NowNodes -> {
                    blockBookNetworkProviderFactory.createNowNodesProvider(blockchain).let(::listOfNotNull)
                }
                ProviderType.BitcoinLike.Blockcypher -> {
                    blockcypherNetworkProviderFactory.create(blockchain).let(::listOfNotNull)
                }
                ProviderType.BitcoinLike.Blockchair -> blockchairNetworkProviderFactory.createProviders(blockchain)
                else -> emptyList()
            }
        }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<BitcoinNetworkProvider> {
        return listOfNotNull(
            blockBookNetworkProviderFactory.createNowNodesProvider(blockchain),
            *blockchairNetworkProviderFactory.createProviders(blockchain).toTypedArray(),
            blockcypherNetworkProviderFactory.create(blockchain),
        )
    }
}