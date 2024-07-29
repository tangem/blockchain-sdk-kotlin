package com.tangem.blockchain.blockchains.icp

import com.tangem.blockchain.blockchains.icp.network.InternetComputerNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.OnlyPublicProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType

internal class InternetComputerProvidersBuilder(
    override val providerTypes: List<ProviderType>,
) : OnlyPublicProvidersBuilder<InternetComputerNetworkProvider>(providerTypes) {

    override fun createProvider(url: String, blockchain: Blockchain): InternetComputerNetworkProvider =
        InternetComputerNetworkProvider(baseUrl = url)
}