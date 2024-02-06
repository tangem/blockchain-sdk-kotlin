package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.extensions.Result
import java.math.BigDecimal

/**
 * Created by Anton Zhilenkov on 31/01/2022.
 */
interface RentProvider {

    suspend fun minimalBalanceForRentExemption(): Result<BigDecimal>

    suspend fun rentAmount(): BigDecimal
}
