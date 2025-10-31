package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.bitcoin.BitcoinFeesCalculator
import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkService
import com.tangem.blockchain.blockchains.dogecoin.DogecoinProvidersBuilder
import com.tangem.blockchain.blockchains.dogecoin.DogecoinWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchain.transactionhistory.TransactionHistoryProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory

internal object DogecoinWalletManagerAssembly : WalletManagerAssembly<DogecoinWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): DogecoinWalletManager {
        with(input.wallet) {
            return DogecoinWalletManager(
                wallet = this,
                transactionBuilder = BitcoinTransactionBuilder(
                    walletPublicKey = publicKey.blockchainKey,
                    blockchain = blockchain,
                    walletAddresses = addresses,
                ),
                networkProvider = BitcoinNetworkService(
                    providers = DogecoinProvidersBuilder(input.providerTypes, input.config).build(blockchain),
                    blockchain = blockchain,
                ),
                transactionHistoryProvider = createTransactionHistoryProvider(input),
                feesCalculator = BitcoinFeesCalculator(blockchain),
            )
        }
    }

    private fun createTransactionHistoryProvider(input: WalletManagerAssemblyInput): TransactionHistoryProvider {
        return if (input.providerTypes.first() == ProviderType.Mock) {
            TransactionHistoryProviderFactory.makeDogecoinMockProvider(input.wallet.blockchain)
        } else {
            TransactionHistoryProviderFactory.makeProvider(input.wallet.blockchain, input.config)
        }
    }
}