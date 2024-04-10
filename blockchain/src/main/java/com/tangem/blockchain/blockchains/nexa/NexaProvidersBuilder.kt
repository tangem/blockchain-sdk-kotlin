package com.tangem.blockchain.blockchains.nexa

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.network.electrum.ElectrumNetworkProvider
import com.tangem.blockchain.network.electrum.ElectrumNetworkProviderFactory

internal object NexaProvidersBuilder : NetworkProvidersBuilder<ElectrumNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<ElectrumNetworkProvider> {
        return listOf(
            ElectrumNetworkProviderFactory.create(
                wssUrl = "wss://onekey-electrum.bitcoinunlimited.info:20004",
                blockchain = blockchain,
            ),
            ElectrumNetworkProviderFactory.create(wssUrl = "wss://electrum.nexa.org:20004", blockchain = blockchain),
        )
    }
}