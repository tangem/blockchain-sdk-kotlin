package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.casper.CasperProvidersBuilder
import com.tangem.blockchain.blockchains.casper.CasperTransactionBuilder
import com.tangem.blockchain.blockchains.casper.CasperWalletManager
import com.tangem.blockchain.blockchains.casper.network.CasperNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

/**
 * Casper [WalletManagerAssembly]
 *
 */
internal object CasperWalletManagerAssembly : WalletManagerAssembly<CasperWalletManager>() {
    override fun make(input: WalletManagerAssemblyInput): CasperWalletManager {
        return with(input.wallet) {
            CasperWalletManager(
                wallet = this,
                networkProvider = CasperNetworkService(
                    providers = CasperProvidersBuilder(
                        providerTypes = input.providerTypes,
                        config = input.config,
                        blockchain = blockchain,
                    ).build(blockchain),
                    blockchain = blockchain,
                ),
                transactionBuilder = CasperTransactionBuilder(this),
                curve = input.curve,
            )
        }
    }
}