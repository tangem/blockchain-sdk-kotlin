package com.tangem.blockchain.common

import java.math.BigDecimal

/**
 * Provides fee resource for paying fee in several blockchains. (ex. Koinos)
 */
interface FeeResourceAmountProvider {

    /**
     * Returns available and max fee resource
     */
    fun getFeeResource(): FeeResource

    /**
     * Returns available and max fee resource by name. In case blockchain has several types of fee resource
     */
    fun getFeeResourceByName(name: String): FeeResource

    /**
     * Is fee enough for a transaction amount
     */
    suspend fun isFeeEnough(amount: BigDecimal, feeName: String? = null): Boolean

    /**
     * Is fee subtractable from amount
     */
    fun isFeeSubtractableFromAmount(): Boolean

    data class FeeResource(
        val value: BigDecimal,
        val maxValue: BigDecimal?,
    )
}