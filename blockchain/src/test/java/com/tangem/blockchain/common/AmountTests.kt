package com.tangem.blockchain.common

import com.google.common.truth.Truth
import org.junit.Test
import java.math.BigDecimal
import kotlin.math.pow

/**
[REDACTED_AUTHOR]
 */
internal class AmountTests {

    @Test
    fun `null amount`() {
        val amount = Amount(blockchain = Blockchain.Bitcoin).copy(value = null)

        try {
            amount.longValue

            error("Should be exception")
        } catch (e: Exception) {
            Truth.assertThat(e).isInstanceOf(IllegalArgumentException::class.java)
            Truth.assertThat(e).hasMessageThat().isEqualTo("Amount value is null")
        }
    }

    @Test
    fun `zero amount`() {
        val amount = Amount(blockchain = Blockchain.Bitcoin).copy(value = BigDecimal.ZERO)

        Truth.assertThat(amount.longValue).isEqualTo(0L)
    }

    @Test
    fun `simple amount`() {
        val value = 1000L
        val amount = Amount(blockchain = Blockchain.Bitcoin).copy(value = BigDecimal(value))

        val expected = value * 10f.pow(Blockchain.Bitcoin.decimals()).toLong()

        Truth.assertThat(amount.longValue).isEqualTo(expected)
    }

    @Test
    fun `long max value amount`() {
        val amount = Amount(blockchain = Blockchain.Bitcoin).copy(value = Long.MAX_VALUE.toBigDecimal())

        try {
            amount.longValue

            error("Should be exception")
        } catch (e: Exception) {
            Truth.assertThat(e).isInstanceOf(ArithmeticException::class.java)
            Truth.assertThat(e).hasMessageThat().isEqualTo("Overflow")
        }
    }

    @Test
    fun `0,1 amount`() {
        val amount = Amount(blockchain = Blockchain.Bitcoin).copy(value = BigDecimal(0.1))

        val expected = 10f.pow(Blockchain.Bitcoin.decimals() - 1).toLong()

        Truth.assertThat(amount.longValue).isEqualTo(expected)
    }

    @Test
    fun `0,000000001 amount`() {
        val amount = Amount(blockchain = Blockchain.Bitcoin).copy(value = BigDecimal(0.000000001))

        // Expected value will be 0,
        // because amount.value.movePointRight(Blockchain.Bitcoin.decimals()) = 0.1

        val expected = 0

        Truth.assertThat(amount.longValue).isEqualTo(expected)
    }
}