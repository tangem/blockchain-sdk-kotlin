package com.tangem.blockchain.common

import com.tangem.blockchain.common.logging.Logger
import java.math.BigDecimal

/**
 * Guards UTXO transaction builders against outputs a node has no business returning: the outpoint index is
 * serialized as uint32 and silently truncated otherwise, and a non-positive amount can only produce an invalid
 * transaction.
 */
internal object UnspentOutputValidator {

    private const val MAX_OUTPUT_INDEX = 0xFFFF_FFFFL

    fun isValid(outputIndex: Long, amount: BigDecimal): Boolean {
        return outputIndex in 0..MAX_OUTPUT_INDEX && amount.signum() > 0
    }

    fun <T> filterValid(outputs: List<T>, outputIndex: (T) -> Long, amount: (T) -> BigDecimal): List<T> {
        val (valid, invalid) = outputs.partition { isValid(outputIndex(it), amount(it)) }
        if (invalid.isNotEmpty()) {
            Logger.e(
                tag = this::class.java.simpleName,
                message = "Dropped ${invalid.size} unspent outputs with an out-of-range index or non-positive amount",
            )
        }
        return valid
    }
}