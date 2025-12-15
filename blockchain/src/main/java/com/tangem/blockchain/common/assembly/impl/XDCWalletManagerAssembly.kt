package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.ethereum.network.EthereumNetworkService
import com.tangem.blockchain.blockchains.ethereum.txbuilder.EthereumTransactionBuilder
import com.tangem.blockchain.blockchains.xdc.XDCProvidersBuilder
import com.tangem.blockchain.blockchains.xdc.XDCWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.network.MultiNetworkProvider
import com.tangem.blockchain.transactionhistory.TransactionHistoryProviderFactory

internal object XDCWalletManagerAssembly : WalletManagerAssembly<XDCWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): XDCWalletManager {
        return with(input.wallet) {
            val multiNetworkProvider = MultiNetworkProvider(
                providers = XDCProvidersBuilder(input.providerTypes, input.config).build(blockchain),
                blockchain = blockchain,
            )
            XDCWalletManager(
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