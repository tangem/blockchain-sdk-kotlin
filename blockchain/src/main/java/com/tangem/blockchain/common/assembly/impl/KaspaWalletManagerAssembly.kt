package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.kaspa.KaspaTransactionBuilder
import com.tangem.blockchain.blockchains.kaspa.KaspaWalletManager
import com.tangem.blockchain.blockchains.kaspa.network.KaspaNetworkProvider
import com.tangem.blockchain.blockchains.kaspa.network.KaspaNetworkService
import com.tangem.blockchain.blockchains.kaspa.network.KaspaRestApiNetworkProvider
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.network.API_KASPA

internal object KaspaWalletManagerAssembly : WalletManagerAssembly<KaspaWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): KaspaWalletManager {
        val providers: List<KaspaNetworkProvider> = buildList {
            add(KaspaRestApiNetworkProvider(API_KASPA))

            input.config.kaspaSecondaryApiUrl
                ?.takeIf { it.isNotBlank() }
                ?.let { url ->
                    add(KaspaRestApiNetworkProvider("$url/"))
                }
        }

        return KaspaWalletManager(
            wallet = input.wallet,
            transactionBuilder = KaspaTransactionBuilder(),
            networkProvider = KaspaNetworkService(providers),
        )
    }
}
