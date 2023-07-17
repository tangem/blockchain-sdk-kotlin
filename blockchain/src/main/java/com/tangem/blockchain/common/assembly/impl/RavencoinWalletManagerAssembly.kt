package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.getBitcoinNetworkProviders
import com.tangem.blockchain.blockchains.bitcoin.getBitcoinTransactionHistoryProvider
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkService
import com.tangem.blockchain.blockchains.ravencoin.RavencoinWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object RavencoinWalletManagerAssembly : WalletManagerAssembly<RavencoinWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): RavencoinWalletManager {
        with(input.wallet) {
            return RavencoinWalletManager(
                wallet = this,
                transactionBuilder = BitcoinTransactionBuilder(
                    walletPublicKey = publicKey.blockchainKey,
                    blockchain = blockchain,
                    walletAddresses = emptySet()
                    // TODO refactoring, make wallet hold address instead of addresskeypair in next task
                ),
                networkProvider = BitcoinNetworkService(
                    providers = blockchain.getBitcoinNetworkProviders(blockchain, input.config)
                ),
                transactionHistoryProvider = blockchain.getBitcoinTransactionHistoryProvider(input.config)
            )
        }
    }

}