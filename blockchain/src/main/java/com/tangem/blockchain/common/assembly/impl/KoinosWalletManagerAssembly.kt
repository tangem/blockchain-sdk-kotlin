package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.koinos.KoinosWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object KoinosWalletManagerAssembly : WalletManagerAssembly<KoinosWalletManager>() {
    override fun make(input: WalletManagerAssemblyInput): KoinosWalletManager {
        return with(input.wallet) {
            KoinosWalletManager(this)
        }
    }
}