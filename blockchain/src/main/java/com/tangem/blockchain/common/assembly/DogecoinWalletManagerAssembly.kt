package com.tangem.blockchain.common.assembly

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.getBitcoinNetworkProviders
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkService
import com.tangem.blockchain.blockchains.dogecoin.DogecoinWalletManager

object DogecoinWalletManagerAssembly : WalletManagerAssembly<DogecoinWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): DogecoinWalletManager {
        with(input.wallet) {
            return DogecoinWalletManager(
                wallet = this,
                transactionBuilder = BitcoinTransactionBuilder(
                    walletPublicKey = publicKey.blockchainKey,
                    blockchain = blockchain,
                    walletAddresses = addresses
                ),
                networkProvider = BitcoinNetworkService(
                    providers = blockchain.getBitcoinNetworkProviders(blockchain, input.config)
                )
            )
        }
    }

}