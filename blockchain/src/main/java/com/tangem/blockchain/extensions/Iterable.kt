package com.tangem.blockchain.extensions

import java.math.BigDecimal

fun Iterable<BigDecimal>.sum() = this.reduce { acc, number -> acc + number }