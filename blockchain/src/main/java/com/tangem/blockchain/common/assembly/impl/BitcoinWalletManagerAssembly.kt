package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.bitcoin.BitcoinFeesCalculator
import com.tangem.blockchain.blockchains.bitcoin.BitcoinProvidersBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory

internal object BitcoinWalletManagerAssembly : WalletManagerAssembly<BitcoinWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): BitcoinWalletManager {
        with(input.wallet) {
            return BitcoinWalletManager(
                wallet = this,
                transactionBuilder = BitcoinTransactionBuilder(
                    walletPublicKey = publicKey.blockchainKey,
                    blockchain = blockchain,
                    walletAddresses = addresses,
                ),
                networkProvider = BitcoinNetworkService(
                    providers = BitcoinProvidersBuilder(providerTypes = input.providerTypes, config = input.config)
                        .build(blockchain),
                ),
                transactionHistoryProvider = TransactionHistoryProviderFactory.makeProvider(blockchain, input.config),
                feesCalculator = BitcoinFeesCalculator(blockchain),
            )
        }
    }
}
