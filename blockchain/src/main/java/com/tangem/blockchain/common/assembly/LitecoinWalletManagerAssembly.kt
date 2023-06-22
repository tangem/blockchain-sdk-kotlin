package com.tangem.blockchain.common.assembly

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.getBitcoinNetworkProviders
import com.tangem.blockchain.blockchains.litecoin.LitecoinNetworkService
import com.tangem.blockchain.blockchains.litecoin.LitecoinWalletManager

object LitecoinWalletManagerAssembly : WalletManagerAssembly<LitecoinWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): LitecoinWalletManager {
        with(input.wallet) {
            return LitecoinWalletManager(
                wallet = this,
                transactionBuilder = BitcoinTransactionBuilder(
                    walletPublicKey = publicKey.blockchainKey,
                    blockchain = blockchain,
                    walletAddresses = addresses
                ),
                networkProvider = LitecoinNetworkService(
                    providers = blockchain.getBitcoinNetworkProviders(blockchain, input.config)
                )
            )
        }
    }

}