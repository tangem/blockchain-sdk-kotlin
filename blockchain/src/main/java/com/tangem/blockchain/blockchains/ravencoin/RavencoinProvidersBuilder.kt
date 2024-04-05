package com.tangem.blockchain.blockchains.ravencoin

import com.tangem.blockchain.blockchains.ravencoin.network.RavencoinNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder

internal class RavencoinProvidersBuilder : NetworkProvidersBuilder<RavencoinNetworkProvider>() {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Ravencoin, Blockchain.RavencoinTestnet)

    override fun createProviders(blockchain: Blockchain): List<RavencoinNetworkProvider> {
        return if (blockchain.isTestnet()) {
            listOf("https://testnet.ravencoin.network/api/")
        } else {
            listOf(
                "https://api.ravencoin.org/api/",
                "https://explorer.rvn.zelcore.io/api/",
            )
        }
            .map(::RavencoinNetworkProvider)
    }
}