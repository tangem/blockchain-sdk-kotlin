package com.tangem.blockchain.common.assembly.impl

import com.tangem.blockchain.blockchains.radiant.RadiantWalletManager
import com.tangem.blockchain.common.assembly.WalletManagerAssembly
import com.tangem.blockchain.common.assembly.WalletManagerAssemblyInput

internal object RadiantWalletManagerAssembly : WalletManagerAssembly<RadiantWalletManager>() {
    override fun make(input: WalletManagerAssemblyInput): RadiantWalletManager = RadiantWalletManager(input.wallet)
}