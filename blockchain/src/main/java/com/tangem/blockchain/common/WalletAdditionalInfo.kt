package com.tangem.blockchain.common

import java.math.BigDecimal

sealed class WalletAdditionalInfo {

    object NoInfo : WalletAdditionalInfo()
// [REDACTED_TODO_COMMENT]
    data class Koinos(
        val mana: BigDecimal,
        val timeToChargeMillis: Long,
    ) : WalletAdditionalInfo()
}
