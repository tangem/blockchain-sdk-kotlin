package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.bitcoin.BitcoinFeesCalculator
import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.factorn.Fact0rnProvidersBuilder
import com.tangem.blockchain.blockchains.factorn.network.Fact0rnNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory

internal object Fact0rnWalletManagerAssembly : WalletManagerAssembly<BitcoinWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): BitcoinWalletManager {
        return with(input.wallet) {
            BitcoinWalletManager(
                wallet = this,
                transactionBuilder = BitcoinTransactionBuilder(
                    walletPublicKey = publicKey.blockchainKey,
                    blockchain = blockchain,
                    walletAddresses = addresses,
                ),
                networkProvider = Fact0rnNetworkService(
                    providers = Fact0rnProvidersBuilder(input.providerTypes).build(blockchain),
                    blockchain = blockchain,
                ),
                transactionHistoryProvider = TransactionHistoryProviderFactory.makeProvider(blockchain, input.config),
                feesCalculator = BitcoinFeesCalculator(blockchain),
            )
        }
    }
}