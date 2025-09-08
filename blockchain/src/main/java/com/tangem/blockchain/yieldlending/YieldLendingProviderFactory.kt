package com.tangem.blockchain.yieldlending

import com.tangem.blockchain.blockchains.ethereum.network.EthereumJsonRpcProvider
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.yieldlending.providers.EthereumYieldLendingProvider

internal class YieldLendingProviderFactory(
    private val dataStorage: AdvancedDataStorage,
) {

    fun makeProvider(wallet: Wallet, networkProvider: MultiNetworkProvider<out NetworkProvider>): YieldLendingProvider {
        val contractAddressFactory = YieldLendingContractAddressFactory(wallet.blockchain)
        return when {
            wallet.blockchain.isEvm() -> EthereumYieldLendingProvider(
                wallet = wallet,
                multiJsonRpcProvider = networkProvider as MultiNetworkProvider<EthereumJsonRpcProvider>, // check?
                contractAddressFactory = contractAddressFactory,
                dataStorage = dataStorage,
            )
            else -> DefaultYieldLendingProvider
        }
    }
}