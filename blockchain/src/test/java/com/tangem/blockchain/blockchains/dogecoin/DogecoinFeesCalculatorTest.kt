package com.tangem.blockchain.blockchains.dogecoin

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.common.Blockchain
import org.junit.Test
import java.math.BigDecimal

internal class DogecoinFeesCalculatorTest {

    private val feesCalculator = DogecoinFeesCalculator(Blockchain.Dogecoin)

    @Test
    fun `fees are clamped to minimal tiers when calculated fees are too low`() {
        // Low fee rates that would produce fees below the Dogecoin minimums
        val txSize = BigDecimal("226")
        val feeRate = BitcoinFee(
            minimalPerKb = BigDecimal("0.001"),
            normalPerKb = BigDecimal("0.01"),
            priorityPerKb = BigDecimal("0.1"),
        )

        val fees = feesCalculator.calculateFees(txSize, feeRate)

        assertThat(fees.minimum.amount.value).isEqualTo(BigDecimal("0.01"))
        assertThat(fees.normal.amount.value).isEqualTo(BigDecimal("0.1"))
        assertThat(fees.priority.amount.value).isEqualTo(BigDecimal("1"))
    }

    @Test
    fun `fees are not clamped when calculated fees exceed minimums`() {
        // High fee rates that produce fees above the minimums
        val txSize = BigDecimal("226")
        val feeRate = BitcoinFee(
            minimalPerKb = BigDecimal("100"),
            normalPerKb = BigDecimal("1000"),
            priorityPerKb = BigDecimal("10000"),
        )

        val fees = feesCalculator.calculateFees(txSize, feeRate)

        assertThat(fees.minimum.amount.value).isGreaterThan(BigDecimal("0.01"))
        assertThat(fees.normal.amount.value).isGreaterThan(BigDecimal("0.1"))
        assertThat(fees.priority.amount.value).isGreaterThan(BigDecimal("1"))
    }
}