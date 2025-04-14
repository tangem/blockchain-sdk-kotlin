package com.tangem.blockchain.common.transaction

import com.google.common.truth.Truth
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class UTXOCollectionUtilsTest {

    private data class UnspentOutput(val amount: BigDecimal)

    private val dustValue = 0.00001.toBigDecimal()

    private val unspentOutputs = listOf(
        UnspentOutput(amount = 1.10.toBigDecimal()),
        UnspentOutput(amount = 4.10.toBigDecimal()),
        UnspentOutput(amount = 2.10.toBigDecimal()),
        UnspentOutput(amount = 12.10.toBigDecimal()),
        UnspentOutput(amount = 10.10.toBigDecimal()),
        UnspentOutput(amount = 3.10.toBigDecimal()),
    )

    @Test
    fun sufficientAmountExact() {
        val expectedOutputs = Result.Success(
            listOf(
                UnspentOutput(amount = 12.10.toBigDecimal()),
            ),
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
        val expectedOutputs = Result.Success(
            listOf(
                UnspentOutput(amount = 12.10.toBigDecimal()),
            ),
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
        val expectedOutputs = Result.Success(
            listOf(
                UnspentOutput(amount = 12.10.toBigDecimal()),
                UnspentOutput(amount = 4.10.toBigDecimal()),
            ),
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
        val expectedOutputs = Result.Success(
            listOf(
                UnspentOutput(amount = 12.10.toBigDecimal()),
                UnspentOutput(amount = 4.10.toBigDecimal()),
            ),
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
        val expectedOutputs = Result.Success(unspentOutputs.sortedByDescending { it.amount })

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
        val expectedOutputs = Result.Success(unspentOutputs.sortedByDescending { it.amount })

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
        val expectedOutputs = Result.Success(
            listOf(
                UnspentOutput(amount = 12.10.toBigDecimal()),
                UnspentOutput(amount = 1.10.toBigDecimal()),
            ),
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
    fun insufficientAmountWithDust() {
        val expectedOutputs = Result.Failure(BlockchainSdkError.TransactionDustChangeError)

        val resOutputs = getMinimumRequiredUTXOsToSend(
            unspentOutputs = unspentOutputs,
            transactionAmount = 32.toBigDecimal(),
            transactionFeeAmount = 0.5999999.toBigDecimal(),
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
}