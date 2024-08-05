package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.createWithPostfixIfContained
import com.tangem.blockchain.common.network.providers.OnlyPublicProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType

internal class FlareProvidersBuilder(
    override val providerTypes: List<ProviderType>,
) : OnlyPublicProvidersBuilder<EthereumJsonRpcProvider>(
    providerTypes = providerTypes,
    testnetProviders = listOf("https://coston2-api.flare.network/ext/C/rpc/"),
) {

    override fun createProvider(url: String, blockchain: Blockchain): EthereumJsonRpcProvider {
        return createWithPostfixIfContained(
            baseUrl = url,
            postfixUrl = POSTFIX_URLS.toTypedArray(),
            create = ::EthereumJsonRpcProvider,
        )
    }

    private companion object {
        val POSTFIX_URLS = listOf("ext/C/rpc", "ext/bc/C/rpc")
    }
}