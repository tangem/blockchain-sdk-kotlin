package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkService
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.telos.TelosProvidersBuilder
import com.tangem.blockchain.blockchains.telos.TelosWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.network.MultiNetworkProvider

internal object TelosWalletManagerAssembly : WalletManagerAssembly<TelosWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): TelosWalletManager {
        with(input.wallet) {
            val multiNetworkProvider = MultiNetworkProvider(
                TelosProvidersBuilder(
                    input.providerTypes,
                    input.config,
                ).build(blockchain),
            )

            return TelosWalletManager(
                wallet = this,
                transactionBuilder = EthereumTransactionBuilder.create(wallet = input.wallet),
                networkProvider = EthereumNetworkService(
                    multiJsonRpcProvider = multiNetworkProvider,
                ),
            )
        }
    }
}