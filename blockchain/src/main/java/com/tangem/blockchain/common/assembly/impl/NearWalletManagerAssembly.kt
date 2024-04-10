package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.near.NearProvidersBuilder
import com.tangem.blockchain.blockchains.near.NearTransactionBuilder
import com.tangem.blockchain.blockchains.near.NearWalletManager
import com.tangem.blockchain.blockchains.near.network.NearNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object NearWalletManagerAssembly : WalletManagerAssembly<NearWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): NearWalletManager {
        return with(input) {
            val networkService = NearNetworkService(
                blockchain = wallet.blockchain,
                providers = NearProvidersBuilder(input.providerTypes, config).build(wallet.blockchain),
            )

            NearWalletManager(
                wallet = wallet,
                networkService = networkService,
                txBuilder = NearTransactionBuilder(wallet.publicKey),
            )
        }
    }
}