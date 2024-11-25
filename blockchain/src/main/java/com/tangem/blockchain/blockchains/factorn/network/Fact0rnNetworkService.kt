package com.tangem.blockchain.blockchains.factorn.network

import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.network.electrum.ElectrumNetworkProvider

internal class Fact0rnNetworkService(providers: List<ElectrumNetworkProvider>) : NetworkProvider {

    override val baseUrl: String
        get() = multiProvider.currentProvider.baseUrl

    private val multiProvider = MultiNetworkProvider(providers)
}