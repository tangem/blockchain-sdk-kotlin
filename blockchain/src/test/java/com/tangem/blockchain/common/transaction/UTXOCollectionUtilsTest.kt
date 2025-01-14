package com.tangem.blockchain.common.transaction

import com.google.common.truth.Truth
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class UTXOCollectionUtilsTest {

    private data class UnspentOutput(val amount: BigDecimal)

    private data class UnspentSatoshiOutput(val amountSatoshi: Long)

    private val dustValue = 0.00001.toBigDecimal()

    private val unspentOutputs = listOf(
        UnspentOutput(amount = 1.10.toBigDecimal()),
        UnspentOutput(amount = 4.10.toBigDecimal()),
        UnspentOutput(amount = 2.10.toBigDecimal()),
        UnspentOutput(amount = 12.10.toBigDecimal()),
        UnspentOutput(amount = 10.10.toBigDecimal()),
        UnspentOutput(amount = 3.10.toBigDecimal()),
    )

    private val unspentSatoshiOutputs = listOf(
        UnspentSatoshiOutput(amountSatoshi = 110),
        UnspentSatoshiOutput(amountSatoshi = 410),
        UnspentSatoshiOutput(amountSatoshi = 210),
        UnspentSatoshiOutput(amountSatoshi = 1210),
        UnspentSatoshiOutput(amountSatoshi = 1010),
        UnspentSatoshiOutput(amountSatoshi = 310),
    )

    @Test
    fun sufficientAmountExact() {
        val expectedOutputs = listOf(
            UnspentOutput(amount = 12.10.toBigDecimal()),
        )

        val resOutputs = getMinimumRequiredUTXOsToSend(
            unspentOutputs = unspentOutputs,
            transactionAmount = 12.10.toBigDecimal(),
            transactionFeeAmount = BigDecimal.ZERO,
            dustValue = dustValue,
            unspentToAmount = { it.amount },
        )

        Truth.assertThat(resOutputs).isEqualTo(expectedOutputs)
    }

    @Test
    fun sufficientAmountExactWithFee() {
        val expectedOutputs = listOf(
            UnspentOutput(amount = 12.10.toBigDecimal()),
        )

        val resOutputs = getMinimumRequiredUTXOsToSend(
            unspentOutputs = unspentOutputs,
            transactionAmount = 10.10.toBigDecimal(),
            transactionFeeAmount = 2.0.toBigDecimal(),
            dustValue = dustValue,
            unspentToAmount = { it.amount },
        )

        Truth.assertThat(resOutputs).isEqualTo(expectedOutputs)
    }

    @Test
    fun sufficientAmountExactMany() {
        val expectedOutputs = listOf(
            UnspentOutput(amount = 12.10.toBigDecimal()),
            UnspentOutput(amount = 4.10.toBigDecimal()),
        )

        val resOutputs = getMinimumRequiredUTXOsToSend(
            unspentOutputs = unspentOutputs,
            transactionAmount = 14.10.toBigDecimal(),
            transactionFeeAmount = 2.10.toBigDecimal(),
            dustValue = dustValue,
            unspentToAmount = { it.amount },
        )

        Truth.assertThat(resOutputs).isEqualTo(expectedOutputs)
    }

    @Test
    fun sufficientAmountMany() {
        val expectedOutputs = listOf(
            UnspentOutput(amount = 12.10.toBigDecimal()),
            UnspentOutput(amount = 4.10.toBigDecimal()),
        )

        val resOutputs = getMinimumRequiredUTXOsToSend(
            unspentOutputs = unspentOutputs,
            transactionAmount = 14.10.toBigDecimal(),
            transactionFeeAmount = 2.0.toBigDecimal(),
            dustValue = dustValue,
            unspentToAmount = { it.amount },
        )

        Truth.assertThat(resOutputs).isEqualTo(expectedOutputs)
    }

    @Test
    fun sufficientAmountMany2() {
        val expectedOutputs = unspentOutputs.sortedByDescending { it.amount }

        val resOutputs = getMinimumRequiredUTXOsToSend(
            unspentOutputs = unspentOutputs,
            transactionAmount = 30.10.toBigDecimal(),
            transactionFeeAmount = 2.0.toBigDecimal(),
            dustValue = dustValue,
            unspentToAmount = { it.amount },
        )

        Truth.assertThat(resOutputs).isEqualTo(expectedOutputs)
    }

    @Test
    fun insufficientAmountMany() {
        val expectedOutputs = unspentOutputs.sortedByDescending { it.amount }

        val resOutputs = getMinimumRequiredUTXOsToSend(
            unspentOutputs = unspentOutputs,
            transactionAmount = 31.10.toBigDecimal(),
            transactionFeeAmount = 2.0.toBigDecimal(),
            dustValue = dustValue,
            unspentToAmount = { it.amount },
        )

        Truth.assertThat(resOutputs).isEqualTo(expectedOutputs)
    }

    @Test
    fun sufficientAmountWithDust() {
        val expectedOutputs = listOf(
            UnspentOutput(amount = 12.10.toBigDecimal()),
            UnspentOutput(amount = 1.10.toBigDecimal()),
        )

        val resOutputs = getMinimumRequiredUTXOsToSend(
            unspentOutputs = unspentOutputs,
            transactionAmount = 10.toBigDecimal(),
            transactionFeeAmount = 2.0999999.toBigDecimal(),
            dustValue = dustValue,
            unspentToAmount = { it.amount },
        )

        Truth.assertThat(resOutputs).isEqualTo(expectedOutputs)
    }

    @Test
    fun negativeAmount() {
        assertThrows<IllegalArgumentException> {
            getMinimumRequiredUTXOsToSend(
                unspentOutputs = unspentOutputs,
                transactionAmount = (-31.10).toBigDecimal(),
                transactionFeeAmount = 2.0.toBigDecimal(),
                dustValue = dustValue,
                unspentToAmount = { it.amount },
            )
        }
        assertThrows<IllegalArgumentException> {
            getMinimumRequiredUTXOsToSend(
                unspentOutputs = unspentOutputs,
                transactionAmount = 31.10.toBigDecimal(),
                transactionFeeAmount = (-2.0).toBigDecimal(),
                dustValue = dustValue,
                unspentToAmount = { it.amount },
            )
        }
        assertThrows<IllegalArgumentException> {
            getMinimumRequiredUTXOsToSend(
                unspentOutputs = unspentOutputs,
                transactionAmount = (-31.10).toBigDecimal(),
                transactionFeeAmount = (-2.0).toBigDecimal(),
                dustValue = dustValue,
                unspentToAmount = { it.amount },
            )
        }
    }

    @Test
    fun sufficientAmountExact_satoshi() {
        val expectedOutputs = listOf(
            UnspentSatoshiOutput(amountSatoshi = 1210),
        )

        val resOutputs = getMinimumRequiredUTXOsToSendSatoshi(
            unspentOutputs = unspentSatoshiOutputs,
            transactionSatoshiAmount = 1210,
            transactionSatoshiFeeAmount = 0,
            dustSatoshiValue = 0,
            unspentToSatoshiAmount = { it.amountSatoshi },
        )

        Truth.assertThat(resOutputs).isEqualTo(expectedOutputs)
    }

    @Test
    fun sufficientAmountExactWithFee_satoshi() {
        val expectedOutputs = listOf(
            UnspentSatoshiOutput(amountSatoshi = 1210),
        )

        val resOutputs = getMinimumRequiredUTXOsToSendSatoshi(
            unspentOutputs = unspentSatoshiOutputs,
            transactionSatoshiAmount = 1010,
            transactionSatoshiFeeAmount = 200,
            dustSatoshiValue = 0,
            unspentToSatoshiAmount = { it.amountSatoshi },
        )

        Truth.assertThat(resOutputs).isEqualTo(expectedOutputs)
    }

    @Test
    fun sufficientAmountExactMany_satoshi() {
        val expectedOutputs = listOf(
            UnspentSatoshiOutput(amountSatoshi = 1210),
            UnspentSatoshiOutput(amountSatoshi = 410),
        )

        val resOutputs = getMinimumRequiredUTXOsToSendSatoshi(
            unspentOutputs = unspentSatoshiOutputs,
            transactionSatoshiAmount = 1410,
            transactionSatoshiFeeAmount = 210,
            dustSatoshiValue = 0,
            unspentToSatoshiAmount = { it.amountSatoshi },
        )

        Truth.assertThat(resOutputs).isEqualTo(expectedOutputs)
    }

    @Test
    fun sufficientAmountMany_satoshi() {
        val expectedOutputs = listOf(
            UnspentSatoshiOutput(amountSatoshi = 1210),
            UnspentSatoshiOutput(amountSatoshi = 410),
        )

        val resOutputs = getMinimumRequiredUTXOsToSendSatoshi(
            unspentOutputs = unspentSatoshiOutputs,
            transactionSatoshiAmount = 1410,
            transactionSatoshiFeeAmount = 200,
            dustSatoshiValue = 0,
            unspentToSatoshiAmount = { it.amountSatoshi },
        )

        Truth.assertThat(resOutputs).isEqualTo(expectedOutputs)
    }

    @Test
    fun sufficientAmountMany2_satoshi() {
        val expectedOutputs = unspentSatoshiOutputs.sortedByDescending { it.amountSatoshi }

        val resOutputs = getMinimumRequiredUTXOsToSendSatoshi(
            unspentOutputs = unspentSatoshiOutputs,
            transactionSatoshiAmount = 3010,
            transactionSatoshiFeeAmount = 200,
            dustSatoshiValue = 0,
            unspentToSatoshiAmount = { it.amountSatoshi },
        )

        Truth.assertThat(resOutputs).isEqualTo(expectedOutputs)
    }

    @Test
    fun insufficientAmountMany_satoshi() {
        val expectedOutputs = unspentSatoshiOutputs.sortedByDescending { it.amountSatoshi }

        val resOutputs = getMinimumRequiredUTXOsToSendSatoshi(
            unspentOutputs = unspentSatoshiOutputs,
            transactionSatoshiAmount = 3110,
            transactionSatoshiFeeAmount = 200,
            dustSatoshiValue = 0,
            unspentToSatoshiAmount = { it.amountSatoshi },
        )

        Truth.assertThat(resOutputs).isEqualTo(expectedOutputs)
    }

    @Test
    fun sufficientAmountWithDust_satoshi() {
        val expectedOutputs = listOf(
            UnspentSatoshiOutput(amountSatoshi = 1210),
            UnspentSatoshiOutput(amountSatoshi = 110),
        )

        val resOutputs = getMinimumRequiredUTXOsToSendSatoshi(
            unspentOutputs = unspentSatoshiOutputs,
            transactionSatoshiAmount = 1000,
            transactionSatoshiFeeAmount = 209,
            dustSatoshiValue = 2,
            unspentToSatoshiAmount = { it.amountSatoshi },
        )

        Truth.assertThat(resOutputs).isEqualTo(expectedOutputs)
    }

    @Test
    fun negativeAmount_satoshi() {
        assertThrows<IllegalArgumentException> {
            getMinimumRequiredUTXOsToSendSatoshi(
                unspentOutputs = unspentSatoshiOutputs,
                transactionSatoshiAmount = -3010,
                transactionSatoshiFeeAmount = 200,
                dustSatoshiValue = 0,
                unspentToSatoshiAmount = { it.amountSatoshi },
            )
        }
        assertThrows<IllegalArgumentException> {
            getMinimumRequiredUTXOsToSendSatoshi(
                unspentOutputs = unspentSatoshiOutputs,
                transactionSatoshiAmount = 3010,
                transactionSatoshiFeeAmount = -200,
                dustSatoshiValue = 0,
                unspentToSatoshiAmount = { it.amountSatoshi },
            )
        }
        assertThrows<IllegalArgumentException> {
            getMinimumRequiredUTXOsToSendSatoshi(
                unspentOutputs = unspentSatoshiOutputs,
                transactionSatoshiAmount = -3010,
                transactionSatoshiFeeAmount = -200,
                dustSatoshiValue = 0,
                unspentToSatoshiAmount = { it.amountSatoshi },
            )
        }
    }
}