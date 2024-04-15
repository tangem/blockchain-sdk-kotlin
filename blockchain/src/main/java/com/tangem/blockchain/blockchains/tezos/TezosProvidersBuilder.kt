package com.tangem.blockchain.blockchains.tezos

import com.tangem.blockchain.blockchains.tezos.network.TezosJsonRpcNetworkProvider
import com.tangem.blockchain.blockchains.tezos.network.TezosNetworkProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.OnlyPublicProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType

internal class TezosProvidersBuilder(
    override val providerTypes: List<ProviderType>,
) : OnlyPublicProvidersBuilder<TezosNetworkProvider>(providerTypes) {

    override fun createProvider(url: String, blockchain: Blockchain) = TezosJsonRpcNetworkProvider(url)
}