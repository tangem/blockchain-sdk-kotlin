package com.tangem.blockchain.common.memo

import java.math.BigInteger

private val UINT64_MAX = BigInteger("FFFFFFFFFFFFFFFF", 16)

fun String.isValidUInt64(): Boolean {
    if (isEmpty() || !all { it.isDigit() }) return false
    return try {
        val number = toBigInteger()
        number >= BigInteger.ZERO && number <= UINT64_MAX
    } catch (_: NumberFormatException) {
        false
    }
}