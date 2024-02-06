package com.tangem.blockchain.common

import com.tangem.blockchain.blockchains.ethereum.EthereumFeesCalculator
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

internal class EthereumFeeCalculatorTest {

    private val feesCalculator = EthereumFeesCalculator()

    @Test
    fun testCalculateFees1() {
        val gasLimit = BigInteger.valueOf(21000)
        val gasPrice = BigInteger.valueOf(12323534123121231)

        val newFees = feesCalculator.calculateFees(
            amountParams = Amount(Blockchain.Ethereum),
            gasLimit = gasLimit,
            gasPrice = gasPrice,
        )

        val minimum = gasLimit.toBigDecimal() * gasPrice.toBigDecimal() * BigDecimal(1)
        val normal = gasLimit.toBigDecimal() * gasPrice.toBigDecimal() * BigDecimal(1.2)
        val priority = gasLimit.toBigDecimal() * gasPrice.toBigDecimal() * BigDecimal(1.5)

        assert(newFees.minimum.amount.value!! - minimum < BigDecimal("1E-18"))
        assert(newFees.normal.amount.value!! - normal < BigDecimal("1E-18"))
        assert(newFees.priority.amount.value!! - priority < BigDecimal("1E-18"))
    }

    @Test
    fun testCalculateFees2() {
        val gasLimit = BigInteger.valueOf(21000)
        val gasPrice = BigInteger.valueOf(213923412000)

        val newFees = feesCalculator.calculateFees(
            amountParams = Amount(Blockchain.Ethereum),
            gasLimit = gasLimit,
            gasPrice = gasPrice,
        )

        val minimum = gasLimit.toBigDecimal() * gasPrice.toBigDecimal() * BigDecimal(1)
        val normal = gasLimit.toBigDecimal() * gasPrice.toBigDecimal() * BigDecimal(1.2)
        val priority = gasLimit.toBigDecimal() * gasPrice.toBigDecimal() * BigDecimal(1.5)

        assert(newFees.minimum.amount.value!! - minimum < BigDecimal("1E-18"))
        assert(newFees.normal.amount.value!! - normal < BigDecimal("1E-18"))
        assert(newFees.priority.amount.value!! - priority < BigDecimal("1E-18"))
    }

    @Test
    fun testCalculateFees3() {
        val gasLimit = BigInteger.valueOf(22000)
        val gasPrice = BigInteger.valueOf(2139812310000)

        val newFees = feesCalculator.calculateFees(
            amountParams = Amount(Blockchain.Ethereum),
            gasLimit = gasLimit,
            gasPrice = gasPrice,
        )

        val minimum = gasLimit.toBigDecimal() * gasPrice.toBigDecimal() * BigDecimal(1)
        val normal = gasLimit.toBigDecimal() * gasPrice.toBigDecimal() * BigDecimal(1.2)
        val priority = gasLimit.toBigDecimal() * gasPrice.toBigDecimal() * BigDecimal(1.5)

        assert(newFees.minimum.amount.value!! - minimum < BigDecimal("1E-18"))
        assert(newFees.normal.amount.value!! - normal < BigDecimal("1E-18"))
        assert(newFees.priority.amount.value!! - priority < BigDecimal("1E-18"))
    }
}
