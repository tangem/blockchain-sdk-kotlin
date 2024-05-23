package com.tangem.blockchain.blockchains.ravencoin

import com.tangem.blockchain.blockchains.ravencoin.network.RavencoinNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.OnlyPublicProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType

internal class RavencoinProvidersBuilder(
    override val providerTypes: List<ProviderType>,
) : OnlyPublicProvidersBuilder<RavencoinNetworkProvider>(
    providerTypes = providerTypes,
    testnetProviders = listOf("https://testnet.ravencoin.network/api/"),
) {

    override fun createProvider(url: String, blockchain: Blockchain) = RavencoinNetworkProvider(baseUrl = url)
}