package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.aptos.AptosWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object AptosWalletManagerAssembly : WalletManagerAssembly<AptosWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): AptosWalletManager {
        return AptosWalletManager(input.wallet)
    }
}