package com.tangem.blockchain.blockchains.litecoin

import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.network.blockbook.BlockBookNetworkProviderFactory
import com.tangem.blockchain.network.blockchair.BlockchairNetworkProviderFactory
import com.tangem.blockchain.network.blockcypher.BlockcypherNetworkProviderFactory

internal class LitecoinProvidersBuilder(
    override val providerTypes: List<ProviderType>,
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<BitcoinNetworkProvider>() {

    private val blockBookProviderFactory by lazy { BlockBookNetworkProviderFactory(config) }
    private val blockchairProviderFactory by lazy { BlockchairNetworkProviderFactory(config) }
    private val blockcypherProviderFactory by lazy { BlockcypherNetworkProviderFactory(config) }

    override fun createProviders(blockchain: Blockchain): List<BitcoinNetworkProvider> {
        return providerTypes.flatMap {
            when (it) {
                ProviderType.NowNodes -> {
                    blockBookProviderFactory.createNowNodesProvider(blockchain).let(::listOfNotNull)
                }
                ProviderType.GetBlock -> {
                    blockBookProviderFactory.createGetBlockProvider(blockchain).let(::listOfNotNull)
                }
                ProviderType.BitcoinLike.Blockcypher -> {
                    blockcypherProviderFactory.create(blockchain).let(::listOfNotNull)
                }
                ProviderType.BitcoinLike.Blockchair -> blockchairProviderFactory.createProviders(blockchain)
                else -> emptyList()
            }
        }
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<BitcoinNetworkProvider> {
        return listOfNotNull(
            blockBookProviderFactory.createNowNodesProvider(blockchain),
            *blockchairProviderFactory.createProviders(blockchain).toTypedArray(),
            blockcypherProviderFactory.create(blockchain),
        )
    }
}