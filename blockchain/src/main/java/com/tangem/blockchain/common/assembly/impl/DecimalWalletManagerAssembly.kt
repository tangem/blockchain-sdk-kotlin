package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.decimal.DecimalNetworkService
import com.tangem.blockchain.blockchains.decimal.DecimalProvidersBuilder
import com.tangem.blockchain.blockchains.decimal.DecimalWalletManager
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionBuilder
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.network.MultiNetworkProvider

internal object DecimalWalletManagerAssembly : WalletManagerAssembly<DecimalWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): DecimalWalletManager {
        return with(input.wallet) {
            val multiNetworkProvider = MultiNetworkProvider(
                DecimalProvidersBuilder(input.providerTypes).build(blockchain),
                blockchain,
            )
            DecimalWalletManager(
                wallet = this,
                transactionBuilder = EthereumTransactionBuilder.create(wallet = input.wallet),
                networkProvider = DecimalNetworkService(
                    multiJsonRpcProvider = multiNetworkProvider,
                ),
            )
        }
    }
}