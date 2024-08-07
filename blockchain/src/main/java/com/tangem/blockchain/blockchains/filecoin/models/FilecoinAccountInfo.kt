package com.tangem.blockchain.blockchains.filecoin.models

import java.math.BigDecimal

/**
 * Filecoin account info
 *
 * @property balance balance
 * @property nonce   nonce
 *
 * @author Andrew Khokhlov on 24/07/2024
 */
data class FilecoinAccountInfo(val balance: BigDecimal, val nonce: Long)
