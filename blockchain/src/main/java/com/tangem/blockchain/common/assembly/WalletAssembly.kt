package com.tangem.blockchain.common.assembly

import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.card.EllipticCurve

abstract class WalletManagerAssembly<out T : WalletManager> {

    abstract fun make(input: WalletManagerAssemblyInput): T

}

class WalletManagerAssemblyInput(
    val wallet: Wallet,
    val config: BlockchainSdkConfig,
    val presetTokens: MutableSet<Token>,
    val curve: EllipticCurve
)