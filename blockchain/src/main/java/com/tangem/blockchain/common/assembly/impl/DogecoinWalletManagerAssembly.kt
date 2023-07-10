package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.getBitcoinNetworkProviders
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkService
import com.tangem.blockchain.blockchains.dogecoin.DogecoinWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object DogecoinWalletManagerAssembly : WalletManagerAssembly<DogecoinWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): DogecoinWalletManager {
        with(input.wallet) {
            return DogecoinWalletManager(
                wallet = this,
                transactionBuilder = BitcoinTransactionBuilder(
                    walletPublicKey = publicKey.blockchainKey,
                    blockchain = blockchain,
                    walletAddresses = emptyList()
                    // TODO refactoring make wallet hold address instead of addresskeypair in next task
                ),
                networkProvider = BitcoinNetworkService(
                    providers = blockchain.getBitcoinNetworkProviders(blockchain, input.config)
                )
            )
        }
    }

}