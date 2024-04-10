package com.tangem.blockchain.blockchains.ravencoin

import com.tangem.blockchain.blockchains.ravencoin.network.RavencoinNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder

internal class RavencoinProvidersBuilder : NetworkProvidersBuilder<RavencoinNetworkProvider>() {

    override fun createProviders(blockchain: Blockchain): List<RavencoinNetworkProvider> {
        return listOf(
            "https://api.ravencoin.org/api/",
            "https://explorer.rvn.zelcore.io/api/",
        )
            .map(::RavencoinNetworkProvider)
    }

    override fun createTestnetProviders(blockchain: Blockchain): List<RavencoinNetworkProvider> {
        return listOf("https://testnet.ravencoin.network/api/")
            .map(::RavencoinNetworkProvider)
    }
}