package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.litecoin.LitecoinFeesCalculator
import com.tangem.blockchain.blockchains.litecoin.LitecoinNetworkService
import com.tangem.blockchain.blockchains.litecoin.LitecoinProvidersBuilder
import com.tangem.blockchain.blockchains.litecoin.LitecoinWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory

internal object LitecoinWalletManagerAssembly : WalletManagerAssembly<LitecoinWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): LitecoinWalletManager {
        with(input.wallet) {
            return LitecoinWalletManager(
                wallet = this,
                transactionBuilder = BitcoinTransactionBuilder(
                    walletPublicKey = publicKey.blockchainKey,
                    blockchain = blockchain,
                    walletAddresses = addresses,
                ),
                networkProvider = LitecoinNetworkService(
                    providers = LitecoinProvidersBuilder(input.providerTypes, input.config).build(blockchain),
                ),
                transactionHistoryProvider = TransactionHistoryProviderFactory.makeProvider(blockchain, input.config),
                feesCalculator = LitecoinFeesCalculator(blockchain),
            )
        }
    }
}