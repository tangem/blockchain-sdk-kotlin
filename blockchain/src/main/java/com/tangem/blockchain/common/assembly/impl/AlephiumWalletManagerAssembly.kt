package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.alephium.AlephiumProvidersBuilder
import com.tangem.blockchain.blockchains.alephium.AlephiumTransactionBuilder
import com.tangem.blockchain.blockchains.alephium.AlephiumWalletManager
import com.tangem.blockchain.blockchains.alephium.network.AlephiumNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object AlephiumWalletManagerAssembly : WalletManagerAssembly<AlephiumWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): AlephiumWalletManager {
        return with(input) {
            AlephiumWalletManager(
                wallet = wallet,
                networkService = AlephiumNetworkService(
                    providers = AlephiumProvidersBuilder(providerTypes = input.providerTypes, config = config)
                        .build(blockchain = wallet.blockchain),
                    blockchain = wallet.blockchain,
                ),
                transactionBuilder = AlephiumTransactionBuilder(wallet.publicKey.blockchainKey, wallet.blockchain),
            )
        }
    }
}