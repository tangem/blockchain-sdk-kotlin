package com.tangem.demo.extensions

import java.math.BigDecimal

/**
 * Created by Anton Zhilenkov on 12/08/2022.
 */
fun BigDecimal.stripZeroPlainString(): String = this.stripTrailingZeros().toPlainString()
