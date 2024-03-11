package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.nexa.NexaWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object NexaWalletManagerAssembly : WalletManagerAssembly<NexaWalletManager>() {
    override fun make(input: WalletManagerAssemblyInput): NexaWalletManager {
        return TODO()
    }
}