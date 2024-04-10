package com.tangem.blockchain.common

import java.math.BigDecimal

sealed class WalletAdditionalInfo {

    object NoInfo : WalletAdditionalInfo()

    // TODO maybe will change (AND-6736)
    data class Koinos(
        val mana: BigDecimal,
        val timeToChargeMillis: Long,
    ) : WalletAdditionalInfo()
}
