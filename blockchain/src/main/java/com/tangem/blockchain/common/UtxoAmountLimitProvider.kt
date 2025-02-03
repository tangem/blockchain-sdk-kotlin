package com.tangem.blockchain.common

import java.math.BigDecimal

/**
 * Provides method to check utxo limit
 */
interface UtxoAmountLimitProvider {

    /**
     * Checks if [amount] and [fee] is not greater than utxo limit. Returns limit otherwise.
     */
    fun checkUtxoAmountLimit(amount: BigDecimal, fee: BigDecimal): UtxoAmountLimit
}

/**
 * @property maxLimit Maximum allowed number of UTXO in single transaction
 * @property maxAmount Maximum allowed amount to be send in single transaction (excluding fee and dust)
 * @property maxAvailableAmount Maximum available amount to spent (UTXO limit in amount value)
 */
data class UtxoAmountLimit(
    val maxLimit: BigDecimal,
    val maxAmount: BigDecimal?,
    val maxAvailableAmount: BigDecimal,
)
