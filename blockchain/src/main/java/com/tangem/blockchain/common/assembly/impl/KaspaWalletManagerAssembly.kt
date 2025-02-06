package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.kaspa.KaspaProvidersBuilder
import com.tangem.blockchain.blockchains.kaspa.KaspaTransactionBuilder
import com.tangem.blockchain.blockchains.kaspa.KaspaWalletManager
import com.tangem.blockchain.blockchains.kaspa.krc20.KaspaKRC20NetworkService
import com.tangem.blockchain.blockchains.kaspa.krc20.model.KaspaKRC20ProvidersBuilder
import com.tangem.blockchain.blockchains.kaspa.network.KaspaNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.datastorage.implementations.AdvancedDataStorage

internal class KaspaWalletManagerAssembly(
    private val dataStorage: AdvancedDataStorage,
) : WalletManagerAssembly<KaspaWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): KaspaWalletManager {
        return with(input.wallet) {
            KaspaWalletManager(
                wallet = this,
                transactionBuilder = KaspaTransactionBuilder(
                    publicKey = publicKey,
                    isTestnet = blockchain.isTestnet(),
                ),
                networkProvider = KaspaNetworkService(
                    providers = KaspaProvidersBuilder(input.providerTypes, input.config).build(blockchain),
                ),
                krc20NetworkProvider = KaspaKRC20NetworkService(
                    providers = KaspaKRC20ProvidersBuilder(input.providerTypes).build(blockchain),
                ),
                dataStorage = dataStorage,
            )
        }
    }
}