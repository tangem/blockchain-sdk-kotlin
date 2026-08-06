package com.tangem.blockchain.blockchains.solana

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Guardrails for the SPL Token-2022 `scaledUiAmountConfig` extension.
 *
 * The multiplier scales the amount that the card actually signs, and it is fetched from the very same RPC
 * provider that reports the balance shown to the user. A provider reporting an understated multiplier inflates
 * the signed transfer while the displayed balance stays plausible - the card has no display, so nothing else
 * would catch it.
 *
 * The extension defines `uiAmount = amount * multiplier / 10^decimals`, so the multiplier can be verified
 * against the balance of the very same token account: to inflate the signed amount a provider has to break
 * that invariant, since a multiplier consistent with the shown balance scales the transfer by the exact factor
 * the user already sees.
 */
internal object SolanaScaledUiAmount {

    /**
     * Relative slack for the comparison. The reported `uiAmount` is rounded by the RPC and, when it comes as a
     * `Double`, carries floating point noise, so an exact match is not expected even from an honest provider.
     */
    private val RELATIVE_TOLERANCE = BigDecimal("0.000001")

    /**
     * Converts the amount the user entered into the on-chain amount the card signs.
     *
     * [multiplier] must be positive - a non-positive one is rejected by the caller before the conversion.
     */
    fun unscale(uiAmount: BigDecimal, multiplier: BigDecimal?, decimals: Int): BigDecimal = when {
        multiplier == null -> uiAmount
        multiplier.compareTo(BigDecimal.ONE) == 0 -> uiAmount
        else -> uiAmount.divide(multiplier, decimals, RoundingMode.DOWN)
    }

    /** Converts an on-chain amount back into the amount shown to the user.*/
    fun scale(onChainAmount: BigDecimal, multiplier: BigDecimal?): BigDecimal = when {
        multiplier == null -> onChainAmount
        multiplier <= BigDecimal.ZERO -> onChainAmount
        multiplier.compareTo(BigDecimal.ONE) == 0 -> onChainAmount
        else -> onChainAmount.multiply(multiplier)
    }

    /**
     * Checks that [multiplier] agrees with the balance reported for the same token account.
     *
     * @param multiplier  multiplier taken from the mint's `scaledUiAmountConfig` extension
     * @param rawBalance  `tokenAmount.amount` - the unscaled on-chain balance
     * @param uiBalance   `tokenAmount.uiAmountString` - the scaled balance shown to the user
     * @param decimals    `tokenAmount.decimals` - the mint decimals
     */
    fun isMultiplierConsistentWithBalance(
        multiplier: BigDecimal,
        rawBalance: BigDecimal,
        uiBalance: BigDecimal,
        decimals: Int,
    ): Boolean {
        val expectedUiBalance = rawBalance.movePointLeft(decimals).multiply(multiplier)

        // On top of the relative slack allow a unit of the last displayed digit, otherwise the rounding of small
        // balances alone would trip the check.
        val tolerance = (expectedUiBalance.abs() * RELATIVE_TOLERANCE)
            .max(BigDecimal.ONE.movePointLeft(decimals))

        return (expectedUiBalance - uiBalance).abs() <= tolerance
    }
}