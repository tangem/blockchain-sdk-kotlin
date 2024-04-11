package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.network.providers.OnlyPublicProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType

internal class MoonriverProvidersBuilder(
    override val providerTypes: List<ProviderType>,
) : OnlyPublicProvidersBuilder<EthereumJsonRpcProvider>(
    providerTypes = providerTypes,
    testnetProviders = listOf("https://rpc.api.moonbase.moonbeam.network/"),
) {

    override fun createProvider(url: String) = EthereumJsonRpcProvider(url)
}