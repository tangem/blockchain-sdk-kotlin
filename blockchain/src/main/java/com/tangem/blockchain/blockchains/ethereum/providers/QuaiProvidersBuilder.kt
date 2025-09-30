package com.tangem.blockchain.blockchains.ethereum.providers

import com.tangem.blockchain.blockchains.quai.QuaiJsonRpcProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.createWithPostfixIfContained
import com.tangem.blockchain.common.network.providers.OnlyPublicProvidersBuilder
import com.tangem.blockchain.common.network.providers.ProviderType

internal class QuaiProvidersBuilder(
    override val providerTypes: List<ProviderType>,
) : OnlyPublicProvidersBuilder<QuaiJsonRpcProvider>(
    providerTypes = providerTypes,
    testnetProviders = listOf("https://orchard.rpc.quai.network/cyprus1/"),
) {

    override fun createProviders(blockchain: Blockchain): List<QuaiJsonRpcProvider> {
        return providerTypes.mapNotNull { type ->
            when (type) {
                is ProviderType.Public -> createPublicProvider(type.url)
                else -> null
            }
        }
    }

    override fun createProvider(url: String, blockchain: Blockchain): QuaiJsonRpcProvider = createPublicProvider(url)

    private fun createPublicProvider(url: String): QuaiJsonRpcProvider {
        return createWithPostfixIfContained(
            baseUrl = url,
            postfixUrl = POSTFIX_URL,
            create = ::QuaiJsonRpcProvider,
        )
    }

    private companion object {
        const val POSTFIX_URL = "cyprus1" // Cyprus-1 is the only active zone
    }
}