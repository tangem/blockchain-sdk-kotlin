package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.polkadot.PolkadotWalletManager
import com.tangem.blockchain.blockchains.polkadot.network.PolkadotNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object PolkadotWalletManagerAssembly : WalletManagerAssembly<PolkadotWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): PolkadotWalletManager {
        return PolkadotWalletManager(
            input.wallet,
            PolkadotNetworkService.network(input.wallet.blockchain)
        )
    }

}