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
 * @property limit Maximum allowed number of UTXO in single transaction
 * @property availableToSpend Allowed amount to participate in single transaction
 * @property availableToSend Amount user can send in single transaction
 */
data class UtxoAmountLimit(
    val limit: BigDecimal,
    val availableToSpend: BigDecimal,
    val availableToSend: BigDecimal?,
)