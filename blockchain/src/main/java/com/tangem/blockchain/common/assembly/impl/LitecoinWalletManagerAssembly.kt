package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.getBitcoinNetworkProviders
import com.tangem.blockchain.blockchains.bitcoin.getBitcoinTransactionHistoryProvider
import com.tangem.blockchain.blockchains.litecoin.LitecoinNetworkService
import com.tangem.blockchain.blockchains.litecoin.LitecoinWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object LitecoinWalletManagerAssembly : WalletManagerAssembly<LitecoinWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): LitecoinWalletManager {
        with(input.wallet) {
            return LitecoinWalletManager(
                wallet = this,
                transactionBuilder = BitcoinTransactionBuilder(
                    walletPublicKey = publicKey.blockchainKey,
                    blockchain = blockchain,
                    walletAddresses = input.wallet.addresses
                ),
                networkProvider = LitecoinNetworkService(
                    providers = blockchain.getBitcoinNetworkProviders(blockchain, input.config)
                ),
                transactionHistoryProvider = blockchain.getBitcoinTransactionHistoryProvider(input.config)
            )
        }
    }

}