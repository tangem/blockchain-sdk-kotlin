package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.nexa.NexaProvidersBuilder
import com.tangem.blockchain.blockchains.nexa.NexaWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.network.electrum.ElectrumNetworkService

internal object NexaWalletManagerAssembly : WalletManagerAssembly<NexaWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): NexaWalletManager {
        return with(input) {
            NexaWalletManager(
                wallet = wallet,
                networkProvider = ElectrumNetworkService(
                    providers = NexaProvidersBuilder(providerTypes).build(wallet.blockchain),
                    blockchain = wallet.blockchain,
                ),
            )
        }
    }
}