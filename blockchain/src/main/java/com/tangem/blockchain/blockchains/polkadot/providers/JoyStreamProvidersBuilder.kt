package com.tangem.blockchain.blockchains.polkadot.providers

import com.tangem.blockchain.blockchains.polkadot.network.PolkadotCombinedProvider
import com.tangem.blockchain.blockchains.polkadot.network.PolkadotNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.OnlyPublicProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType

internal class JoyStreamProvidersBuilder(
    override val providerTypes: List<ProviderType>,
) : OnlyPublicProvidersBuilder<PolkadotNetworkProvider>(providerTypes) {

    override fun createProvider(url: String, blockchain: Blockchain): PolkadotNetworkProvider {
        return PolkadotCombinedProvider(baseUrl = url, blockchain = blockchain)
    }
}