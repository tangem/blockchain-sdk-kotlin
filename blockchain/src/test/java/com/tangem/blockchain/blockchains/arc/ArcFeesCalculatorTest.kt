package com.tangem.blockchain.blockchains.arc

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumFeesCalculator
import com.tangem.blockchain.blockchains.ethereum.network.EthereumFeeHistory
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Tests for Arc fee calculation: the chain requires at least 20 gwei of maxFeePerGas and reports a zero priority fee,
 * which is replaced with a hardcoded 5 gwei.
 */
internal class ArcFeesCalculatorTest {

    private val arcCalculator = EthereumFeesCalculator(Blockchain.Arc)
    private val ethereumCalculator = EthereumFeesCalculator(Blockchain.Ethereum)

    @Test
    fun `Arc raises zero priority fee to 5 gwei`() {
        val fees = arcCalculator.calculateEip1559Fees(
            amountParams = Amount(Blockchain.Arc),
            gasLimit = GAS_LIMIT,
            feeHistory = zeroPriorityFeeHistory(),
        )

        listOf(fees.minimum, fees.normal, fees.priority).forEach { fee ->
            Truth.assertThat((fee as Fee.Ethereum.EIP1559).priorityFee).isEqualTo(FIVE_GWEI)
        }
    }

    @Test
    fun `Arc keeps maxFeePerGas at least 20 gwei`() {
        val fees = arcCalculator.calculateEip1559Fees(
            amountParams = Amount(Blockchain.Arc),
            gasLimit = GAS_LIMIT,
            feeHistory = EthereumFeeHistory.Common(
                baseFee = BigDecimal.ZERO,
                lowPriorityFee = BigDecimal.ZERO,
                marketPriorityFee = BigDecimal.ZERO,
                fastPriorityFee = BigDecimal.ZERO,
            ),
        )

        listOf(fees.minimum, fees.normal, fees.priority).forEach { fee ->
            Truth.assertThat((fee as Fee.Ethereum.EIP1559).maxFeePerGas).isAtLeast(TWENTY_GWEI)
        }
    }

    @Test
    fun `Arc fee amount is wei scaled to 18 decimals`() {
        val fees = arcCalculator.calculateEip1559Fees(
            amountParams = Amount(Blockchain.Arc),
            gasLimit = GAS_LIMIT,
            feeHistory = zeroPriorityFeeHistory(),
        )

        val minimum = fees.minimum as Fee.Ethereum.EIP1559

        Truth.assertThat(minimum.maxFeePerGas).isEqualTo(TWENTY_GWEI)
        Truth.assertThat(minimum.amount.decimals).isEqualTo(18)
        Truth.assertThat(minimum.amount.value).isEqualTo(BigDecimal("0.000420000000000000"))
    }

    @Test
    fun `Ethereum fee calculation is not affected by Arc gas floors`() {
        val fees = ethereumCalculator.calculateEip1559Fees(
            amountParams = Amount(Blockchain.Ethereum),
            gasLimit = GAS_LIMIT,
            feeHistory = zeroPriorityFeeHistory(),
        )

        val minimum = fees.minimum as Fee.Ethereum.EIP1559

        Truth.assertThat(minimum.priorityFee).isEqualTo(BigInteger.ZERO)
        Truth.assertThat(minimum.maxFeePerGas).isEqualTo(BASE_FEE)
    }

    private fun zeroPriorityFeeHistory() = EthereumFeeHistory.Common(
        baseFee = BigDecimal(BASE_FEE),
        lowPriorityFee = BigDecimal.ZERO,
        marketPriorityFee = BigDecimal.ZERO,
        fastPriorityFee = BigDecimal.ZERO,
    )

    private companion object {
        val GAS_LIMIT: BigInteger = BigInteger.valueOf(21_000)
        val BASE_FEE: BigInteger = BigInteger.valueOf(7_000_000_000)
        val TWENTY_GWEI: BigInteger = BigInteger.valueOf(20_000_000_000)
        val FIVE_GWEI: BigInteger = BigInteger.valueOf(5_000_000_000)
    }
}