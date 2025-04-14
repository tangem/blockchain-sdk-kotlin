package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.koinos.KoinosContractIdHolder
import com.tangem.blockchain.blockchains.koinos.KoinosProviderBuilder
import com.tangem.blockchain.blockchains.koinos.KoinosTransactionBuilder
import com.tangem.blockchain.blockchains.koinos.KoinosWalletManager
import com.tangem.blockchain.blockchains.koinos.network.KoinosNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory

internal object KoinosWalletManagerAssembly : WalletManagerAssembly<KoinosWalletManager>() {
    override fun make(input: WalletManagerAssemblyInput): KoinosWalletManager {
        return with(input.wallet) {
            val networkService = KoinosNetworkService(
                providers = KoinosProviderBuilder(
                    providerTypes = input.providerTypes,
                    config = input.config,
                ).build(blockchain),
            )
            KoinosWalletManager(
                wallet = this,
                networkService = networkService,
                transactionBuilder = KoinosTransactionBuilder(
                    isTestnet = blockchain.isTestnet(),
                ),
                // No implementation for now
                // See KoinosTransactionHistoryProvider deprecation doc
                transactionHistoryProvider = TransactionHistoryProviderFactory.makeProvider(blockchain, input.config),
                contractIdHolder = KoinosContractIdHolder(
                    isTestnet = blockchain.isTestnet(),
                    loadKoinContractId = { networkService.getContractId() },
                ),
            )
        }
    }
}