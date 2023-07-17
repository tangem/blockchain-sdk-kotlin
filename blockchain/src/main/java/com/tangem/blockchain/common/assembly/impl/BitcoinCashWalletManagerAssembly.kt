package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.bitcoin.getBitcoinNetworkProviders
import com.tangem.blockchain.blockchains.bitcoin.getBitcoinTransactionHistoryProvider
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkService
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoincash.BitcoinCashWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object BitcoinCashWalletManagerAssembly : WalletManagerAssembly<BitcoinCashWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): BitcoinCashWalletManager  {
        with(input.wallet) {
            return BitcoinCashWalletManager(
                wallet = this,
                transactionBuilder = BitcoinCashTransactionBuilder(
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