package com.tangem.blockchain.yieldsupply

import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.yieldsupply.providers.EthereumYieldSupplyProvider

/**
 *  Factory for creating instances of [YieldSupplyProvider] based on the wallet's blockchain.
 */
internal class YieldSupplyProviderFactory(
    private val dataStorage: AdvancedDataStorage,
) {

    @Suppress("UNCHECKED_CAST")
    fun makeProvider(wallet: Wallet, networkProvider: MultiNetworkProvider<out NetworkProvider>): YieldSupplyProvider {
        val contractAddressFactory = YieldSupplyContractAddressFactory(wallet.blockchain)
        return when {
            wallet.blockchain.isEvm() -> EthereumYieldSupplyProvider(
                wallet = wallet,
                multiJsonRpcProvider = networkProvider as MultiNetworkProvider<EthereumJsonRpcProvider>,
                contractAddressFactory = contractAddressFactory,
                dataStorage = dataStorage,
            )
            else -> DefaultYieldSupplyProvider
        }
    }
}