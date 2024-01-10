package com.tangem.blockchain.common

import java.math.BigDecimal

/**
 * Provides method to check utxo limit
 */
interface UtxoAmountLimitProvider {

    /**
     * Checks if [amount] and [fee] is not greater than utxo limit. Returns limit otherwise.
     */
    fun checkUtxoAmountLimit(amount: BigDecimal, fee: BigDecimal): UtxoAmountLimit?
}

data class UtxoAmountLimit(
    val maxLimit: BigDecimal,
    val maxAmount: BigDecimal,
)
