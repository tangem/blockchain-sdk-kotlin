package com.tangem.blockchain.common.network.providers

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.common.logging.Logger

/**
 * Network providers builder
 *
 * @param T instance of [NetworkProvider]
 *
[REDACTED_AUTHOR]
 */
internal abstract class NetworkProvidersBuilder<out T : NetworkProvider> {

    /** List of [ProviderType] */
    protected abstract val providerTypes: List<ProviderType>

    /** Create list of mainnet [NetworkProvider] instances */
    protected abstract fun createProviders(blockchain: Blockchain): List<T>

    /** Create list of testnet [NetworkProvider] instances */
    protected open fun createTestnetProviders(blockchain: Blockchain): List<T> = emptyList()

    /** Create list of [NetworkProvider] instances for [blockchain] */
    fun build(blockchain: Blockchain): List<T> {
        val providers = if (blockchain.isTestnet()) createTestnetProviders(blockchain) else createProviders(blockchain)

        return providers.ifEmpty {
            Logger.e(NetworkProvidersBuilder::class.java.simpleName, "No providers found for $blockchain")
            emptyList()
        }
    }
}