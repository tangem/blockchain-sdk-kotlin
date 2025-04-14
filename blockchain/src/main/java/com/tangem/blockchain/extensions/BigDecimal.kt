package com.tangem.blockchain.extensions

import java.math.BigDecimal

fun max(a: BigDecimal, b: BigDecimal): BigDecimal {
    return if (a > b) a else b
}

fun BigDecimal?.orZero() = this ?: BigDecimal.ZERO