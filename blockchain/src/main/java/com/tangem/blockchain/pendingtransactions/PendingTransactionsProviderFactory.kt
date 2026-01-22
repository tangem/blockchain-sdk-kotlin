package com.tangem.blockchain.pendingtransactions

import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
import com.tangem.blockchain.common.di.DepsContainer
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.pendingtransactions.providers.EthereumPendingTransactionsProvider

/**
 * Factory for creating instances of [PendingTransactionsProvider] based on the wallet's blockchain.
 */
internal class PendingTransactionsProviderFactory(
    private val dataStorage: AdvancedDataStorage,
) {

    @Suppress("UNCHECKED_CAST")
    fun makeProvider(
        wallet: Wallet,
        networkProvider: MultiNetworkProvider<out NetworkProvider>,
        networkProviderMap: Map<ProviderType, NetworkProvider>,
    ): PendingTransactionsProvider {
        if (!DepsContainer.blockchainFeatureToggles.isPendingTransactionsEnabled) {
            return DefaultPendingTransactionsProvider
        }

        return when {
            wallet.blockchain.isEvm() -> {
                val storage = PendingTransactionStorage(wallet, dataStorage)
                EthereumPendingTransactionsProvider(
                    wallet = wallet,
                    multiJsonRpcProvider = networkProvider as MultiNetworkProvider<EthereumJsonRpcProvider>,
                    storage = storage,
                    networkProviderMap = networkProviderMap,
                )
            }
            else -> DefaultPendingTransactionsProvider
        }
    }
}