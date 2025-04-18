package com.tangem.blockchain.blockchains.bitcoin

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import org.junit.Test
import java.math.BigDecimal

internal class BitcoinFeesCalculatorTest {

    private val bitcoin: Blockchain = Blockchain.Bitcoin

    private val feesCalculator: BitcoinFeesCalculator = BitcoinFeesCalculator(bitcoin)

    private val amount = Amount(
        currencySymbol = "BTC",
        value = null,
        decimals = 8,
        type = AmountType.Coin,
    )

    @Test
    fun calculateFeesTest1() {
        val txSize = BigDecimal("222")
        val minimalPerKb = BigDecimal("0.00012294")
        val normalPerKb = BigDecimal("0.00018980")
        val priorityPerKb = BigDecimal("0.00034559")
        val feeRate = BitcoinFee(
            minimalPerKb = minimalPerKb,
            normalPerKb = normalPerKb,
            priorityPerKb = priorityPerKb,
        )
        val expected = TransactionFee.Choosable(
            normal = Fee.Bitcoin(
                amount = amount.copy(value = BigDecimal("0.00004218")),
                satoshiPerByte = BigDecimal(19),
                txSize = txSize,
            ),
            minimum = Fee.Bitcoin(
                amount = amount.copy(value = BigDecimal("0.00002886")),
                satoshiPerByte = BigDecimal(13),
                txSize = txSize,
            ),
            priority = Fee.Bitcoin(
                amount = amount.copy(value = BigDecimal("0.00007770")),
                satoshiPerByte = BigDecimal(35),
                txSize = txSize,
            ),
        )

        val fees = feesCalculator.calculateFees(txSize, feeRate)

        Truth.assertThat(fees).isEqualTo(expected)
    }

    @Test
    fun calculateFeesTest2() {
        val txSize = BigDecimal("32604")
        val minimalPerKb = BigDecimal("0.00012294")
        val normalPerKb = BigDecimal("0.00018980")
        val priorityPerKb = BigDecimal("0.00034559")
        val feeRate = BitcoinFee(
            minimalPerKb = minimalPerKb,
            normalPerKb = normalPerKb,
            priorityPerKb = priorityPerKb,
        )
        val expected = TransactionFee.Choosable(
            normal = Fee.Bitcoin(
                amount = amount.copy(value = BigDecimal("0.00619476")),
                satoshiPerByte = BigDecimal(19),
                txSize = txSize,
            ),
            minimum = Fee.Bitcoin(
                amount = amount.copy(value = BigDecimal("0.00423852")),
                satoshiPerByte = BigDecimal(13),
                txSize = txSize,
            ),
            priority = Fee.Bitcoin(
                amount = amount.copy(value = BigDecimal("0.01141140")),
                satoshiPerByte = BigDecimal(35),
                txSize = txSize,
            ),
        )

        val fees = feesCalculator.calculateFees(txSize, feeRate)

        Truth.assertThat(fees).isEqualTo(expected)
    }

    @Test
    fun calculateFeesTest3() {
        val txSize = BigDecimal("32604")
        val minimalPerKb = BigDecimal("0.00012294")
        val normalPerKb = BigDecimal("0.00018980")
        val priorityPerKb = BigDecimal("0.00034559")
        val feeRate = BitcoinFee(
            minimalPerKb = minimalPerKb,
            normalPerKb = normalPerKb,
            priorityPerKb = priorityPerKb,
        )
        val expected = TransactionFee.Choosable(
            minimum = Fee.Bitcoin(
                amount = amount.copy(value = BigDecimal("0.00604320")),
                satoshiPerByte = normalPerKb,
                txSize = txSize,
            ),
            priority = Fee.Bitcoin(
                amount = amount.copy(value = BigDecimal("0.00391439")),
                satoshiPerByte = minimalPerKb,
                txSize = txSize,
            ),
            normal = Fee.Bitcoin(
                amount = amount.copy(value = BigDecimal("0.01100353")),
                satoshiPerByte = priorityPerKb,
                txSize = txSize,
            ),
        )

        val fees = feesCalculator.calculateFees(txSize, feeRate)

        Truth.assertThat(fees).isNotEqualTo(expected)
    }

    @Test
    fun calculateFeesTest4() {
        val txSize = BigDecimal("32604")
        val minimalPerKb = BigDecimal("0.00012294")
        val normalPerKb = BigDecimal("0.00018980")
        val priorityPerKb = BigDecimal("0.00034559")
        val feeRate = BitcoinFee(
            minimalPerKb = minimalPerKb,
            normalPerKb = normalPerKb,
            priorityPerKb = priorityPerKb,
        )
        val expected = TransactionFee.Choosable(
            normal = Fee.Bitcoin(
                amount = amount.copy(value = BigDecimal("0.00002665")),
                satoshiPerByte = normalPerKb,
                txSize = txSize,
            ),
            minimum = Fee.Bitcoin(
                amount = amount.copy(value = BigDecimal("0.00004114")),
                satoshiPerByte = minimalPerKb,
                txSize = txSize,
            ),
            priority = Fee.Bitcoin(
                amount = amount.copy(value = BigDecimal("0.00007492")),
                satoshiPerByte = priorityPerKb,
                txSize = txSize,
            ),
        )

        val fees = feesCalculator.calculateFees(txSize, feeRate)

        Truth.assertThat(fees).isNotEqualTo(expected)
    }
}