package com.tangem.blockchain.extensions

import org.bitcoinj.core.ECKey
import org.kethereum.contract.abi.types.leftPadToFixedSize
import java.math.BigDecimal
import java.math.BigInteger

fun BigInteger.toCanonicalised(): BigInteger {
    if (!this.isCanonical()) ECKey.CURVE.n - this
    return this
}

fun BigInteger.isCanonical(): Boolean = this <= ECKey.HALF_CURVE_ORDER

fun BigInteger?.toBigDecimalOrDefault(default: BigDecimal = BigDecimal.ZERO): BigDecimal =
    this?.toBigDecimal() ?: default

fun BigInteger?.orZero(): BigInteger = this ?: BigInteger.ZERO

fun BigInteger.toFixedSizeBytes(fixedSize: Int = 32) = toByteArray().removeLeadingZeros().leftPadToFixedSize(
    fixedSize = fixedSize,
)