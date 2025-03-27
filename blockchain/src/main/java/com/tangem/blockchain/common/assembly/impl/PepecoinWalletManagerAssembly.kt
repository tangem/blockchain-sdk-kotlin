package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.pepecoin.PepecoinFeeCalculator
import com.tangem.blockchain.blockchains.pepecoin.PepecoinProvidersBuilder
import com.tangem.blockchain.blockchains.pepecoin.network.PepecoinNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory

internal object PepecoinWalletManagerAssembly : WalletManagerAssembly<BitcoinWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): BitcoinWalletManager {
        return with(input.wallet) {
            BitcoinWalletManager(
                wallet = this,
                transactionBuilder = BitcoinTransactionBuilder(
                    walletPublicKey = publicKey.blockchainKey,
                    blockchain = blockchain,
                    walletAddresses = addresses,
                ),
                networkProvider = PepecoinNetworkService(
                    providers = PepecoinProvidersBuilder(input.providerTypes).build(blockchain),
                    blockchain = blockchain,
                ),
                transactionHistoryProvider = TransactionHistoryProviderFactory.makeProvider(blockchain, input.config),
                feesCalculator = PepecoinFeeCalculator(blockchain),
            )
        }
    }
}