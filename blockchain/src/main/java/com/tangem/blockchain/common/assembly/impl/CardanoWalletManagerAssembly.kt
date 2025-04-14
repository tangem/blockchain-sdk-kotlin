package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.cardano.CardanoProvidersBuilder
import com.tangem.blockchain.blockchains.cardano.CardanoTransactionBuilder
import com.tangem.blockchain.blockchains.cardano.CardanoWalletManager
import com.tangem.blockchain.blockchains.cardano.network.CardanoNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object CardanoWalletManagerAssembly : WalletManagerAssembly<CardanoWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): CardanoWalletManager {
        return with(input.wallet) {
            CardanoWalletManager(
                wallet = this,
                transactionBuilder = CardanoTransactionBuilder(wallet = this),
                networkProvider = CardanoNetworkService(
                    providers = CardanoProvidersBuilder(providerTypes = input.providerTypes, config = input.config)
                        .build(blockchain = blockchain),
                ),
            )
        }
    }
}