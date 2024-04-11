package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.koinos.KoinosProviderBuilder
import com.tangem.blockchain.blockchains.koinos.KoinosTransactionBuilder
import com.tangem.blockchain.blockchains.koinos.KoinosWalletManager
import com.tangem.blockchain.blockchains.koinos.network.KoinosNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object KoinosWalletManagerAssembly : WalletManagerAssembly<KoinosWalletManager>() {
    override fun make(input: WalletManagerAssemblyInput): KoinosWalletManager {
        return with(input.wallet) {
            KoinosWalletManager(
                wallet = this,
                networkService = KoinosNetworkService(
                    providers = KoinosProviderBuilder().build(blockchain),
                ),
                transactionBuilder = KoinosTransactionBuilder(isTestnet = blockchain.isTestnet()),
            )
        }
    }
}