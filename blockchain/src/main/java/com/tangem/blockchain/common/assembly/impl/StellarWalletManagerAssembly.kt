package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.stellar.StellarNetworkService
import com.tangem.blockchain.blockchains.stellar.StellarProvidersBuilder
import com.tangem.blockchain.blockchains.stellar.StellarTransactionBuilder
import com.tangem.blockchain.blockchains.stellar.StellarWalletManager
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object StellarWalletManagerAssembly : WalletManagerAssembly<StellarWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): StellarWalletManager {
        with(input.wallet) {
            val isTestnet = blockchain == Blockchain.StellarTestnet

            val networkService = StellarNetworkService(
                isTestnet = isTestnet,
                providers = StellarProvidersBuilder(input.providerTypes, input.config).build(blockchain),
            )

            return StellarWalletManager(
                wallet = this,
                transactionBuilder = StellarTransactionBuilder(networkService, publicKey.blockchainKey),
                networkProvider = networkService,
            )
        }
    }
}