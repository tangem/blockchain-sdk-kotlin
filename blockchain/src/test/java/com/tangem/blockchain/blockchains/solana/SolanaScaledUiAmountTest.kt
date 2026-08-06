package com.tangem.blockchain.blockchains.solana

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal

internal class SolanaScaledUiAmountTest {

    @Test
    fun `GIVEN no multiplier WHEN unscale THEN amount is left untouched`() {
        assertThat(SolanaScaledUiAmount.unscale(BigDecimal("1000"), multiplier = null, decimals = DECIMALS))
            .isEqualTo(BigDecimal("1000"))
    }

    @Test
    fun `GIVEN neutral multiplier WHEN unscale THEN amount is left untouched`() {
        assertThat(SolanaScaledUiAmount.unscale(BigDecimal("1000"), BigDecimal.ONE, DECIMALS))
            .isEqualTo(BigDecimal("1000"))
    }

    @Test
    fun `GIVEN multiplier above one WHEN unscale THEN amount is divided`() {
        // Arrange
        // The mint scales 100 on-chain tokens up to 1000 on screen, so sending 1000 signs 100
        val actual = SolanaScaledUiAmount.unscale(BigDecimal("1000"), BigDecimal("10"), DECIMALS)

        // Assert
        assertThat(actual).isEqualTo(BigDecimal("100.000000"))
    }

    @Test
    fun `GIVEN multiplier below one WHEN unscale THEN amount is inflated`() {
        val actual = SolanaScaledUiAmount.unscale(BigDecimal("1000"), BigDecimal("0.5"), DECIMALS)

        assertThat(actual).isEqualTo(BigDecimal("2000.000000"))
    }

    @Test
    fun `GIVEN amount not divisible by the multiplier WHEN unscale THEN it is rounded down`() {
        // Arrange
        // 10 / 3 never terminates, and rounding up would sign more than the user approved
        val actual = SolanaScaledUiAmount.unscale(BigDecimal("10"), BigDecimal("3"), DECIMALS)

        // Assert
        assertThat(actual).isEqualTo(BigDecimal("3.333333"))
    }

    @Test
    fun `GIVEN a multiplier WHEN unscale and scale back THEN the original amount is restored`() {
        // Arrange
        val displayed = BigDecimal("1234.56789")
        val multiplier = BigDecimal("7.5")

        // Act
        val onChain = SolanaScaledUiAmount.unscale(displayed, multiplier, decimals = 8)
        val restored = SolanaScaledUiAmount.scale(onChain, multiplier)

        // Assert
        assertThat(restored.compareTo(displayed)).isEqualTo(0)
    }

    @Test
    fun `GIVEN multiplier matching the balance WHEN checked THEN consistent`() {
        // Arrange
        // 1000 tokens on-chain, multiplier 1.5 -> 1500 shown to the user
        val isConsistent = SolanaScaledUiAmount.isMultiplierConsistentWithBalance(
            multiplier = BigDecimal("1.5"),
            rawBalance = BigDecimal("1000000000"),
            uiBalance = BigDecimal("1500"),
            decimals = DECIMALS,
        )

        // Assert
        assertThat(isConsistent).isTrue()
    }

    @Test
    fun `GIVEN neutral multiplier WHEN checked THEN consistent`() {
        val isConsistent = SolanaScaledUiAmount.isMultiplierConsistentWithBalance(
            multiplier = BigDecimal.ONE,
            rawBalance = BigDecimal("1000000000"),
            uiBalance = BigDecimal("1000"),
            decimals = DECIMALS,
        )

        assertThat(isConsistent).isTrue()
    }

    @Test
    fun `GIVEN understated multiplier from a hostile provider WHEN checked THEN inconsistent`() {
        // Arrange
        // The report's scenario: the balance is reported with the true multiplier of 1.0, while the mint account
        // claims 0.001, which would inflate the signed transfer 1000x.
        val isConsistent = SolanaScaledUiAmount.isMultiplierConsistentWithBalance(
            multiplier = BigDecimal("0.001"),
            rawBalance = BigDecimal("1000000000"),
            uiBalance = BigDecimal("1000"),
            decimals = DECIMALS,
        )

        // Assert
        assertThat(isConsistent).isFalse()
    }

    @Test
    fun `GIVEN fabricated multiplier for a mint without the extension WHEN checked THEN inconsistent`() {
        val isConsistent = SolanaScaledUiAmount.isMultiplierConsistentWithBalance(
            multiplier = BigDecimal("0.5"),
            rawBalance = BigDecimal("1000000000"),
            uiBalance = BigDecimal("1000"),
            decimals = DECIMALS,
        )

        assertThat(isConsistent).isFalse()
    }

    @Test
    fun `GIVEN ui balance rounded by the rpc WHEN checked THEN consistent`() {
        // Arrange
        // 1 raw unit * 1.0000001 = 0.0000010000001, the rpc reports it rounded to the mint decimals
        val isConsistent = SolanaScaledUiAmount.isMultiplierConsistentWithBalance(
            multiplier = BigDecimal("1.0000001"),
            rawBalance = BigDecimal.ONE,
            uiBalance = BigDecimal("0.000001"),
            decimals = DECIMALS,
        )

        // Assert
        assertThat(isConsistent).isTrue()
    }

    @Test
    fun `GIVEN large balance with floating point noise WHEN checked THEN consistent`() {
        // Arrange
        // uiAmount arrives as a Double, so the last digits of a large balance are not exact
        val isConsistent = SolanaScaledUiAmount.isMultiplierConsistentWithBalance(
            multiplier = BigDecimal("1.2345678"),
            rawBalance = BigDecimal("123456789000000"),
            uiBalance = BigDecimal("152415776.390794"),
            decimals = DECIMALS,
        )

        // Assert
        assertThat(isConsistent).isTrue()
    }

    @Test
    fun `GIVEN zero balance WHEN checked THEN consistent`() {
        val isConsistent = SolanaScaledUiAmount.isMultiplierConsistentWithBalance(
            multiplier = BigDecimal("2"),
            rawBalance = BigDecimal.ZERO,
            uiBalance = BigDecimal.ZERO,
            decimals = DECIMALS,
        )

        assertThat(isConsistent).isTrue()
    }

    @Test
    fun `GIVEN inflated multiplier making the user undersend WHEN checked THEN inconsistent`() {
        val isConsistent = SolanaScaledUiAmount.isMultiplierConsistentWithBalance(
            multiplier = BigDecimal("1000"),
            rawBalance = BigDecimal("1000000000"),
            uiBalance = BigDecimal("1000"),
            decimals = DECIMALS,
        )

        assertThat(isConsistent).isFalse()
    }

    private companion object {
        const val DECIMALS = 6
    }
}