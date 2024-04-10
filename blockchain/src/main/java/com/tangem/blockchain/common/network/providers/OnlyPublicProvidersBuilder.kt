package com.tangem.blockchain.common.network.providers

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.NetworkProvider

/**
 * Network providers builder for creating only public [NetworkProvider] instances
 *
 * @property providerTypes    list of [ProviderType]
 * @property testnetProviders list of testnet providers
 */
internal abstract class OnlyPublicProvidersBuilder<T : NetworkProvider>(
    override val providerTypes: List<ProviderType>,
    private val testnetProviders: List<String> = emptyList(),
) : NetworkProvidersBuilder<T>() {

    /** Create public [NetworkProvider] instance by [url] */
    abstract fun createProvider(url: String): T?

    override fun createProviders(blockchain: Blockchain): List<T> = providerTypes.mapToProviders()

    override fun createTestnetProviders(blockchain: Blockchain): List<T> {
        return testnetProviders
            .map(ProviderType::Public)
            .mapToProviders()
    }

    private fun List<ProviderType>.mapToProviders(): List<T> {
        return mapNotNull {
            if (it is ProviderType.Public) createProvider(url = it.url) else null
        }
    }
}