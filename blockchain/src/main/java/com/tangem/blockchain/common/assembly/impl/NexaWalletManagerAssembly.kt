package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.nexa.NexaWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput
import com.tangem.blockchain.network.electrum.ElectrumNetworkService
import com.tangem.blockchain.network.electrum.getElectrumNetworkProviders

internal object NexaWalletManagerAssembly : WalletManagerAssembly<NexaWalletManager>() {
    override fun make(input: WalletManagerAssemblyInput): NexaWalletManager {
        with(input.wallet) {
            return NexaWalletManager(
                wallet = this,
                networkProvider = ElectrumNetworkService(
                    providers = blockchain.getElectrumNetworkProviders(),
                ),
            )
        }
    }
}