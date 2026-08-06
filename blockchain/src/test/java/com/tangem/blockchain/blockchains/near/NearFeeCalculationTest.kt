package com.tangem.blockchain.blockchains.near

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.near.network.NearGasPrice
import com.tangem.blockchain.blockchains.near.network.Yocto
import com.tangem.blockchain.blockchains.near.network.api.ProtocolConfigResult
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Regression cover for [REDACTED_TASK_KEY]. Gas costs are the mainnet values of protocol version 86.
 *
 * The expected implicit fee is the one nearcore itself reported: replaying a rejected send-all transaction through
 * `send_tx` answered `NotEnoughBalance` with `cost = 3978914888538187500000000` for a deposit of
 * `3971307446081937500000000`, which leaves exactly `7607442456250000000000` of fee.
 */
internal class NearFeeCalculationTest {

    private val gasPrice = NearGasPrice(yoctoGasPrice = Yocto(BigInteger.valueOf(100_000_000)), blockHash = "hash")

    @Test
    fun `fee for an implicit destination charges execution gas at the purchase price`() {
        val fee = protocolConfig().calculateSendFundsFee(gasPrice, isImplicitAccount = true)

        assertThat(fee.value).isEqualTo(BigInteger("7607442456250000000000"))
    }

    @Test
    fun `fee for a named destination charges execution gas at the purchase price`() {
        val fee = protocolConfig().calculateSendFundsFee(gasPrice, isImplicitAccount = false)

        assertThat(fee.value).isEqualTo(BigInteger("245500818750000000000"))
    }

    @Test
    fun `send all from the [REDACTED_TASK_KEY] account survives what nearcore actually charges`() {
        val balance = BigInteger("2009165010462500000000000")
        val storageStaking = BigInteger("1820000000000000000000")
        val chargedByNearcore = BigInteger("7607442456250000000000")

        val fee = protocolConfig().calculateSendFundsFee(gasPrice, isImplicitAccount = true).value
        val maxAmount = balance - storageStaking - fee

        assertThat(balance - maxAmount - chargedByNearcore).isAtLeast(storageStaking)
    }

    @Test
    fun `the amount rejected in the logs would not be offered as max any more`() {
        val balance = BigInteger("3973985010462500000000000")
        val storageStaking = BigInteger("1820000000000000000000")
        val rejectedAmount = BigInteger("3971307446081937500000000")

        val fee = protocolConfig().calculateSendFundsFee(gasPrice, isImplicitAccount = true).value
        val maxAmount = balance - storageStaking - fee

        assertThat(maxAmount).isLessThan(rejectedAmount)
    }

    @Test
    fun `block price above the purchase price is used for the execution part`() {
        val highGasPrice = NearGasPrice(yoctoGasPrice = Yocto(BigInteger.valueOf(5_000_000_000)), blockHash = "hash")

        val fee = protocolConfig().calculateSendFundsFee(highGasPrice, isImplicitAccount = false)

        assertThat(fee.value).isEqualTo(BigInteger.valueOf(223_182_562_500L * 2).multiply(BigInteger("5000000000")))
    }

    @Test
    fun `the purchase price is used, not the block price floor`() {
        val testnetShaped = protocolConfig(minGasPrice = "5000", minGasPurchasePrice = "1000000000")

        val fee = testnetShaped.calculateSendFundsFee(gasPrice, isImplicitAccount = true)

        assertThat(fee.value).isEqualTo(BigInteger("7607442456250000000000"))
    }

    @Test
    fun `a chain without a purchase price charges everything at the block price`() {
        val legacy = protocolConfig(minGasPurchasePrice = "0")

        val fee = legacy.calculateSendFundsFee(gasPrice, isImplicitAccount = true)

        assertThat(fee.value).isEqualTo(BigInteger("834989537500000000000"))
    }

    private fun protocolConfig(
        minGasPrice: String = "1000000000",
        minGasPurchasePrice: String = "1000000000",
    ): ProtocolConfigResult = ProtocolConfigResult(
        chainId = "mainnet",
        protocolVersion = "86",
        genesisHeight = 9_820_210,
        maxGasPrice = BigDecimal("10000000000000000000000"),
        minGasPrice = BigDecimal(minGasPrice),
        runtimeConfig = ProtocolConfigResult.RuntimeConfig(
            storageAmountPerByte = BigDecimal("10000000000000000000"),
            minGasPurchasePrice = BigDecimal(minGasPurchasePrice),
            transactionCosts = ProtocolConfigResult.TransactionCost(
                actionReceiptCreationConfig = cost(sendNotSir = 108_059_500_000, execution = 108_059_500_000),
                actionCreationConfig = ProtocolConfigResult.ActionCreationConfig(
                    transferCost = cost(sendNotSir = 115_123_062_500, execution = 115_123_062_500),
                    createAccountCost = cost(sendNotSir = 500_000_000_000, execution = 7_200_000_000_000),
                    addKeyCost = ProtocolConfigResult.AddKeyCost(
                        fullAccessCost = cost(sendNotSir = 101_765_125_000, execution = 101_765_125_000),
                        functionCallCost = cost(sendNotSir = 102_217_625_000, execution = 102_217_625_000),
                    ),
                ),
            ),
        ),
    )

    private fun cost(sendNotSir: Long, execution: Long) = ProtocolConfigResult.CostConfig(
        sendSir = sendNotSir,
        sendNotSir = sendNotSir,
        execution = execution,
    )
}