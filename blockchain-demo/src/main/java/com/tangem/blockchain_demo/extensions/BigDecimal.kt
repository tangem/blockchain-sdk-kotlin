package com.tangem.blockchain_demo.extensions

import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
fun BigDecimal.stripZeroPlainString(): String = this.stripTrailingZeros().toPlainString()