package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.polkadot.PolkadotTransactionBuilder
import com.tangem.blockchain.blockchains.polkadot.PolkadotWalletManager
import com.tangem.blockchain.blockchains.polkadot.extensions.getPolkadotHosts
import com.tangem.blockchain.blockchains.polkadot.network.PolkadotCombinedProvider
import com.tangem.blockchain.blockchains.polkadot.network.PolkadotNetworkService
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object PolkadotWalletManagerAssembly : WalletManagerAssembly<PolkadotWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): PolkadotWalletManager {
        return PolkadotWalletManager(
            wallet = input.wallet,
            transactionBuilder = PolkadotTransactionBuilder(input.wallet.blockchain),
            networkProvider = PolkadotNetworkService(
                input.wallet.blockchain.getPolkadotHosts()
                    .map { PolkadotCombinedProvider(input.wallet.blockchain.decimals(), it) }
            )
        )
    }

}