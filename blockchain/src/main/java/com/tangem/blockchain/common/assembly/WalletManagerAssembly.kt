package com.tangem.blockchain.common.assembly

import com.tangem.blockchain.common.*

/**
 * Creates WalletManager instance
 */
internal abstract class WalletManagerAssembly<out T : WalletManager> {

    internal abstract fun make(input: WalletManagerAssemblyInput): T

}
