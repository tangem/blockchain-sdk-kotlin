package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkService
import com.tangem.blockchain.blockchains.clore.CloreFeesCalculator
import com.tangem.blockchain.blockchains.clore.CloreProvidersBuilder
import com.tangem.blockchain.blockchains.clore.CloreWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory

internal object CloreWalletManagerAssembly : WalletManagerAssembly<CloreWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): CloreWalletManager {
        with(input.wallet) {
            return CloreWalletManager(
                wallet = this,
                transactionBuilder = BitcoinTransactionBuilder(
                    walletPublicKey = publicKey.blockchainKey,
                    blockchain = blockchain,
                    walletAddresses = addresses,
                ),
                networkProvider = BitcoinNetworkService(
                    providers = CloreProvidersBuilder(input.providerTypes, input.config).build(blockchain),
                    blockchain = blockchain,
                ),
                transactionHistoryProvider = TransactionHistoryProviderFactory.makeProvider(blockchain, input.config),
                feesCalculator = CloreFeesCalculator(blockchain),
            )
        }
    }
}