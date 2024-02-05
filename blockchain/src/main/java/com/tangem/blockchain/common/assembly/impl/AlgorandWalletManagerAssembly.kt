package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.algorand.AlgorandWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object AlgorandWalletManagerAssembly : WalletManagerAssembly<AlgorandWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): AlgorandWalletManager {
        return AlgorandWalletManager(
            wallet = input.wallet,
        )
    }
}