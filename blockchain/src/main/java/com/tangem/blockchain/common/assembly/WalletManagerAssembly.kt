package com.tangem.blockchain.common.assembly

import com.tangem.blockchain.common.*

/**
 * Creates WalletManager instance
 */
abstract class WalletManagerAssembly<out T : WalletManager> {

    abstract fun make(input: WalletManagerAssemblyInput): T

}
