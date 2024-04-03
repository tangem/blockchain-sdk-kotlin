package com.tangem.blockchain.common.network.providers

import android.util.Log
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.NetworkProvider

/**
 * Network providers builder
 *
 * @param T instance of [NetworkProvider]
 *
[REDACTED_AUTHOR]
 */
abstract class NetworkProvidersBuilder<out T : NetworkProvider> {

    /** Supported blockchains */
    protected abstract val supportedBlockchains: List<Blockchain>

    /** Create list of [NetworkProvider] instances for [blockchain] */
    fun build(blockchain: Blockchain): List<T> {
        if (blockchain !in supportedBlockchains) {
            Log.e(NetworkProvidersBuilder::class.simpleName, "Unsupported blockchain: $blockchain")
            return emptyList()
        }

        return createProviders(blockchain)
    }

    /** Protected method for implementation of creating of network providers for supported [blockchain] */
    protected abstract fun createProviders(blockchain: Blockchain): List<T>
}