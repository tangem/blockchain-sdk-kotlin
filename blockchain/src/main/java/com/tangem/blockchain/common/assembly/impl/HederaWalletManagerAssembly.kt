package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.hedera.HederaProvidersBuilder
import com.tangem.blockchain.blockchains.hedera.HederaTransactionBuilder
import com.tangem.blockchain.blockchains.hedera.HederaWalletManager
import com.tangem.blockchain.blockchains.hedera.network.HederaNetworkService
import com.tangem.blockchain.common.AccountCreator
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage

internal class HederaWalletManagerAssembly(
    private val dataStorage: AdvancedDataStorage,
    private val accountCreator: AccountCreator,
) : WalletManagerAssembly<HederaWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): HederaWalletManager {
        return with(input) {
            HederaWalletManager(
                wallet = wallet,
                transactionBuilder = HederaTransactionBuilder(curve = curve, wallet = wallet),
                networkProvider = HederaNetworkService(
                    hederaNetworkProviders = HederaProvidersBuilder(input.providerTypes, config)
                        .build(wallet.blockchain),
                ),
                dataStorage = dataStorage,
                accountCreator = accountCreator,
            )
        }
    }
}