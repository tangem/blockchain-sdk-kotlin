package com.tangem.blockchain.blockchains.sui.bcs

import java.math.BigDecimal

/**
 * Converts a whole amount of the smallest units to the bit pattern of a Move `u64`, which may exceed [Long.MAX_VALUE].
 * Throws [ArithmeticException] for a fractional amount and [IllegalArgumentException] for one out of the u64 range.
 */
internal fun BigDecimal.toU64Bits(): Long {
    val value = toBigIntegerExact()
    require(value.signum() >= 0 && value.bitLength() <= Long.SIZE_BITS) { "Amount doesn't fit u64: $this" }
    return value.toLong()
}