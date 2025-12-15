package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.chia.ChiaProvidersBuilder
import com.tangem.blockchain.blockchains.chia.ChiaTransactionBuilder
import com.tangem.blockchain.blockchains.chia.ChiaWalletManager
import com.tangem.blockchain.blockchains.chia.network.ChiaNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object ChiaWalletManagerAssembly : WalletManagerAssembly<ChiaWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): ChiaWalletManager {
        return with(input.wallet) {
            ChiaWalletManager(
                wallet = this,
                networkProvider = ChiaNetworkService(
                    chiaNetworkProviders = ChiaProvidersBuilder(input.providerTypes, input.config).build(blockchain),
                    blockchain = blockchain,
                ),
                transactionBuilder = ChiaTransactionBuilder(publicKey.blockchainKey, blockchain),
            )
        }
    }
}