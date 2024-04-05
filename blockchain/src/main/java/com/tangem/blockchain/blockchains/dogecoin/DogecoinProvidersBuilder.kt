package com.tangem.blockchain.blockchains.dogecoin

import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.network.blockbook.BlockBookNetworkProviderFactory
import com.tangem.blockchain.network.blockchair.BlockchairNetworkProviderFactory
import com.tangem.blockchain.network.blockcypher.BlockcypherNetworkProviderFactory

internal class DogecoinProvidersBuilder(
    private val config: BlockchainSdkConfig,
) : NetworkProvidersBuilder<BitcoinNetworkProvider>() {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Dogecoin)

    private val blockBookNetworkProviderFactory by lazy { BlockBookNetworkProviderFactory(config) }
    private val blockchairNetworkProviderFactory by lazy { BlockchairNetworkProviderFactory(config) }
    private val blockcypherNetworkProviderFactory by lazy { BlockcypherNetworkProviderFactory(config) }

    override fun createProviders(blockchain: Blockchain): List<BitcoinNetworkProvider> {
        return listOfNotNull(
            blockBookNetworkProviderFactory.createNowNodesProvider(blockchain),
            blockBookNetworkProviderFactory.createGetBlockProvider(blockchain),
            *blockchairNetworkProviderFactory.createProviders(blockchain).toTypedArray(),
            blockcypherNetworkProviderFactory.create(blockchain),
        )
    }
}