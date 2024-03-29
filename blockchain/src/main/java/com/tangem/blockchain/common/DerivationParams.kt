package com.tangem.blockchain.common

import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.crypto.hdWallet.DerivationPath

sealed class DerivationParams {

    data class Default(val style: DerivationStyle) : DerivationParams()

    data class Custom(val path: DerivationPath) : DerivationParams()

    fun getPath(blockchain: Blockchain): DerivationPath? {
        return when (this) {
            is Custom -> path
            is Default -> blockchain.derivationPath(style)
        }
    }
}
