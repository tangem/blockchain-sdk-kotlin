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

        val oldFees = feesCalculator.calculateFeesUsingOldMethod(
            amountParams = Amount(Blockchain.Ethereum), gasLimit = gasLimit, gasPrice
        )
        val newFees =
            feesCalculator.calculateFees(amountParams = Amount(Blockchain.Ethereum), gasLimit, gasPrice)


        assert((oldFees.minimum.amount.value!! - newFees.minimum.amount.value!!) < BigDecimal("1E-18"))
        assert((oldFees.normal.amount.value!! - newFees.normal.amount.value!!) < BigDecimal("1E-18"))
        assert((oldFees.priority.amount.value!! - newFees.priority.amount.value!!) < BigDecimal("1E-18"))
    }

    @Test
    fun testCalculateFees2() {
        val gasLimit = BigInteger.valueOf(21000)
        val gasPrice = BigInteger.valueOf(213923412000)

        val oldFees = feesCalculator.calculateFeesUsingOldMethod(
            amountParams = Amount(Blockchain.Ethereum), gasLimit = gasLimit, gasPrice
        )
        val newFees =
            feesCalculator.calculateFees(amountParams = Amount(Blockchain.Ethereum), gasLimit, gasPrice)


        assert((oldFees.minimum.amount.value!! - newFees.minimum.amount.value!!) < BigDecimal("1E-18"))
        assert((oldFees.normal.amount.value!! - newFees.normal.amount.value!!) < BigDecimal("1E-18"))
        assert((oldFees.priority.amount.value!! - newFees.priority.amount.value!!) < BigDecimal("1E-18"))
    }

    @Test
    fun testCalculateFees3() {
        val gasLimit = BigInteger.valueOf(22000)
        val gasPrice = BigInteger.valueOf(2139812310000)

        val oldFees = feesCalculator.calculateFeesUsingOldMethod(
            amountParams = Amount(Blockchain.Ethereum), gasLimit = gasLimit, gasPrice
        )
        val newFees =
            feesCalculator.calculateFees(amountParams = Amount(Blockchain.Ethereum), gasLimit, gasPrice)


        assert((oldFees.minimum.amount.value!! - newFees.minimum.amount.value!!) < BigDecimal("1E-18"))
        assert((oldFees.normal.amount.value!! - newFees.normal.amount.value!!) < BigDecimal("1E-18"))
        assert((oldFees.priority.amount.value!! - newFees.priority.amount.value!!) < BigDecimal("1E-18"))
    }


}