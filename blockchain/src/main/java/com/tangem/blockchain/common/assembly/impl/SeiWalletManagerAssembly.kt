package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.sei.SeiWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object SeiWalletManagerAssembly : WalletManagerAssembly<SeiWalletManager>() {

    override fun make(input: WalletManagerAssemblyInput): SeiWalletManager {
        return SeiWalletManager(wallet = input.wallet)
    }
}