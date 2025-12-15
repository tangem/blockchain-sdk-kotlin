package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkService
import com.tangem.blockchain.blockchains.ethereum.providers.MantleProvidersBuilder
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.mantle.MantleWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory

internal object MantleWalletManagerAssembly : WalletManagerAssembly<MantleWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): MantleWalletManager {
        return with(input.wallet) {
            val multiNetworkProvider = MultiNetworkProvider(
                providers = MantleProvidersBuilder(input.providerTypes).build(blockchain),
                blockchain = blockchain,
            )

            MantleWalletManager(
                wallet = this,
                transactionBuilder = EthereumTransactionBuilder.create(wallet = input.wallet),
                networkProvider = EthereumNetworkService(
                    multiJsonRpcProvider = multiNetworkProvider,
                ),
                transactionHistoryProvider = TransactionHistoryProviderFactory.makeProvider(blockchain, input.config),
            )
        }
    }
}