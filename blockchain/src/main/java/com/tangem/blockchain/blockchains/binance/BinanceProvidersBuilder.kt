package com.tangem.blockchain.blockchains.binance

import com.tangem.blockchain.blockchains.binance.network.BinanceNetworkProvider
import com.tangem.blockchain.blockchains.binance.network.BinanceNetworkService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder

internal object BinanceProvidersBuilder : NetworkProvidersBuilder<BinanceNetworkProvider>() {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Binance)

    override fun createProviders(blockchain: Blockchain): List<BinanceNetworkProvider> {
        return listOf(BinanceNetworkService(blockchain.isTestnet()))
    }
}