package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.extensions.Result
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
interface RentProvider {

    suspend fun minimalBalanceForRentExemption(): Result<BigDecimal>

    suspend fun rentAmount(): BigDecimal
}