package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.kaspa.KaspaProvidersBuilder
import com.tangem.blockchain.blockchains.kaspa.KaspaTransactionBuilder
import com.tangem.blockchain.blockchains.kaspa.KaspaWalletManager
import com.tangem.blockchain.blockchains.kaspa.network.KaspaNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object KaspaWalletManagerAssembly : WalletManagerAssembly<KaspaWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): KaspaWalletManager {
        return with(input.wallet) {
            KaspaWalletManager(
                wallet = this,
                transactionBuilder = KaspaTransactionBuilder(),
                networkProvider = KaspaNetworkService(
                    providers = KaspaProvidersBuilder(input.providerTypes, input.config).build(blockchain),
                ),
            )
        }
    }
}