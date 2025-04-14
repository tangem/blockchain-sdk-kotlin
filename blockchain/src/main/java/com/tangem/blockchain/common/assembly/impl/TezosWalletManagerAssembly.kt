package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.tezos.TezosProvidersBuilder
import com.tangem.blockchain.blockchains.tezos.TezosTransactionBuilder
import com.tangem.blockchain.blockchains.tezos.TezosWalletManager
import com.tangem.blockchain.blockchains.tezos.network.TezosNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object TezosWalletManagerAssembly : WalletManagerAssembly<TezosWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): TezosWalletManager {
        return with(input.wallet) {
            TezosWalletManager(
                wallet = this,
                transactionBuilder = TezosTransactionBuilder(publicKey.blockchainKey, input.curve),
                networkProvider = TezosNetworkService(
                    providers = TezosProvidersBuilder(input.providerTypes, input.config).build(blockchain),
                ),
                curve = input.curve,
            )
        }
    }
}