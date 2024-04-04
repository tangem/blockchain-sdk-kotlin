package com.tangem.blockchain.blockchains.radiant

import com.tangem.blockchain.blockchains.radiant.network.RadiantNetworkService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.NetworkProvidersBuilder
import com.tangem.blockchain.network.BlockchainSdkRetrofitBuilder
import com.tangem.blockchain.network.electrum.ElectrumNetworkProvider
import com.tangem.blockchain.network.electrum.ElectrumNetworkProviderFactory

internal object RadiantProvidersBuilder : NetworkProvidersBuilder<ElectrumNetworkProvider>() {

    override val supportedBlockchains: List<Blockchain> = listOf(Blockchain.Radiant)

    override fun createProviders(blockchain: Blockchain): List<ElectrumNetworkProvider> {
        return listOf(
            ElectrumNetworkProviderFactory.create(
                wssUrl = "wss://electrumx-01-ssl.radiant4people.com:51002",
                blockchain = blockchain,
                supportedProtocolVersion = RadiantNetworkService.SUPPORTED_SERVER_VERSION,
                okHttpClient = BlockchainSdkRetrofitBuilder.createOkhttpClientForRadiant(),
            ),
        )
    }
}