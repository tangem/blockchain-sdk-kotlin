package com.tangem.blockchain.common

import java.math.BigDecimal

/**
 * Provides methods for getting reserve amount and checking if account is funded with reserved amount.
 */
interface ReserveAmountProvider {
    /** Returns reserve amount required for Blockchain  */
    fun getReserveAmount(): BigDecimal

    /** Returns if [destinationAddress] is created and funded with reserve */
    suspend fun isAccountFunded(destinationAddress: String): Boolean
}
