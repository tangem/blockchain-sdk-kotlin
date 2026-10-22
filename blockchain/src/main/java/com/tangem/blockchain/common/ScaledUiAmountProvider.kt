package com.tangem.blockchain.common

import com.tangem.blockchain.extensions.Result
import java.math.BigDecimal

/**
 * Provides the factor between the token amount shown to the user and the amount the chain operates on, for tokens
 * whose balance is reported already scaled (Solana Token-2022 `scaledUiAmountConfig`).
 *
 * Transfers built by the SDK unscale the amount themselves. Anything that hands an amount to a transaction built
 * elsewhere, e.g. a swap compiled by a provider, has to divide it by this multiplier first.
 */
interface ScaledUiAmountProvider {

    /** Returns the multiplier in force for [token], or `null` when the shown and on-chain amounts coincide. */
    suspend fun getScaledUiAmountMultiplier(token: Token): Result<BigDecimal?>
}