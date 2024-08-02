package com.tangem.blockchain.blockchains.filecoin.models

import java.math.BigDecimal

/**
 * Filecoin account info
 *
 * @property balance balance
 * @property nonce   nonce
 *
[REDACTED_AUTHOR]
 */
data class FilecoinAccountInfo(val balance: BigDecimal, val nonce: Long)