package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.tezos.TezosTransactionBuilder
import com.tangem.blockchain.blockchains.tezos.TezosWalletManager
import com.tangem.blockchain.blockchains.tezos.network.TezosJsonRpcNetworkProvider
import com.tangem.blockchain.blockchains.tezos.network.TezosNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.network.API_TEZOS_BLOCKSCALE
import com.tangem.blockchain.network.API_TEZOS_ECAD
import com.tangem.blockchain.network.API_TEZOS_SMARTPY

internal object TezosWalletManagerAssembly : WalletManagerAssembly<TezosWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): TezosWalletManager {
        with(input.wallet) {
            val providers = listOf(
                TezosJsonRpcNetworkProvider(API_TEZOS_BLOCKSCALE),
                TezosJsonRpcNetworkProvider(API_TEZOS_SMARTPY),
                TezosJsonRpcNetworkProvider(API_TEZOS_ECAD),
            )

            return TezosWalletManager(
                this,
                TezosTransactionBuilder(publicKey.blockchainKey, input.curve),
                TezosNetworkService(providers),
                input.curve,
            )
        }
    }
}
