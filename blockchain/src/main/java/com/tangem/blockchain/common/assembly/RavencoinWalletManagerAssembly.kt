package com.tangem.blockchain.common.assembly

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.getBitcoinNetworkProviders
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkService
import com.tangem.blockchain.blockchains.ravencoin.RavencoinWalletManager

object RavencoinWalletManagerAssembly : WalletManagerAssembly<RavencoinWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): RavencoinWalletManager {
        with(input.wallet) {
            return RavencoinWalletManager(
                wallet = this,
                transactionBuilder = BitcoinTransactionBuilder(
                    walletPublicKey = publicKey.blockchainKey,
                    blockchain = blockchain,
                    walletAddresses = addresses,
                ),
                networkProvider = BitcoinNetworkService(
                    providers = blockchain.getBitcoinNetworkProviders(blockchain, input.config)
                )
            )
        }
    }

}