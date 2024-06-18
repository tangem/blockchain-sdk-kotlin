package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.OnlyPublicProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType

internal class AuroraProvidersBuilder(
    override val providerTypes: List<ProviderType>,
) : OnlyPublicProvidersBuilder<EthereumJsonRpcProvider>(
    providerTypes = providerTypes,
    testnetProviders = listOf("https://testnet.aurora.dev/"),
) {

    override fun createProvider(url: String, blockchain: Blockchain): EthereumJsonRpcProvider {
        return EthereumJsonRpcProvider.createWithPostfixIfContained(baseUrl = url, postfixUrl = BASE_URL_LAST_PATH)
    }

    private companion object {
        const val BASE_URL_LAST_PATH = "aurora"
    }
}