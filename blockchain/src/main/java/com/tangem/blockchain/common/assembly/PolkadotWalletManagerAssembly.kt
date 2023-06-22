package com.tangem.blockchain.common.assembly

import com.tangem.blockchain.blockchains.polkadot.PolkadotWalletManager
import com.tangem.blockchain.blockchains.polkadot.network.PolkadotNetworkService

object PolkadotWalletManagerAssembly : WalletManagerAssembly<PolkadotWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): PolkadotWalletManager {
        return PolkadotWalletManager(
            input.wallet,
            PolkadotNetworkService.network(input.wallet.blockchain)
        )
    }

}