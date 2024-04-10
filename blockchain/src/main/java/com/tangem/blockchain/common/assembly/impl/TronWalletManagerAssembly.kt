package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.tron.TronProvidersBuilder
import com.tangem.blockchain.blockchains.tron.TronTransactionBuilder
import com.tangem.blockchain.blockchains.tron.TronWalletManager
import com.tangem.blockchain.blockchains.tron.network.TronNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.txhistory.getTransactionHistoryProvider

internal object TronWalletManagerAssembly : WalletManagerAssembly<TronWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): TronWalletManager {
        with(input.wallet) {
            return TronWalletManager(
                wallet = this,
                transactionHistoryProvider = blockchain.getTransactionHistoryProvider(input.config),
                transactionBuilder = TronTransactionBuilder(blockchain),
                networkService = TronNetworkService(
                    rpcNetworkProviders = TronProvidersBuilder(input.providerTypes, input.config).build(blockchain),
                    blockchain = input.wallet.blockchain,
                ),
            )
        }
    }
}
