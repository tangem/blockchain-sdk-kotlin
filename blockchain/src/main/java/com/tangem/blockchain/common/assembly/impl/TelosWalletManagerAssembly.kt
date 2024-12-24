package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkService
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.telos.TelosProvidersBuilder
import com.tangem.blockchain.blockchains.telos.TelosWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object TelosWalletManagerAssembly : WalletManagerAssembly<TelosWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): TelosWalletManager {
        with(input.wallet) {
            return TelosWalletManager(
                wallet = this,
                transactionBuilder = EthereumTransactionBuilder.create(wallet = input.wallet),
                networkProvider = EthereumNetworkService(
                    jsonRpcProviders = TelosProvidersBuilder(
                        input.providerTypes,
                        input.config,
                    ).build(blockchain),
                ),
            )
        }
    }
}