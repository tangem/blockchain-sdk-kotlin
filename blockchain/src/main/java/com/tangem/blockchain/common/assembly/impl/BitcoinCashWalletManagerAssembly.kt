package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkService
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashProvidersBuilder
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.txhistory.getTransactionHistoryProvider

internal object BitcoinCashWalletManagerAssembly : WalletManagerAssembly<BitcoinCashWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): BitcoinCashWalletManager {
        with(input.wallet) {
            return BitcoinCashWalletManager(
                wallet = this,
                transactionBuilder = BitcoinCashTransactionBuilder(
                    walletPublicKey = publicKey.blockchainKey,
                    blockchain = blockchain,
                ),
                networkProvider = BitcoinNetworkService(
                    providers = BitcoinCashProvidersBuilder(providerTypes = input.providerTypes, config = input.config)
                        .build(blockchain),
                ),
                transactionHistoryProvider = blockchain.getTransactionHistoryProvider(input.config),
            )
        }
    }
}