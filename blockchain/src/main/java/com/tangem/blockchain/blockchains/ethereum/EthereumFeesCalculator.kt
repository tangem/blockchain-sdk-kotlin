package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

class EthereumFeesCalculator {

    @Deprecated("use calculateFees() instead of this")
    internal fun calculateFeesUsingOldMethod(
        amountParams: Amount,
        gasLimit: BigInteger,
        gasPrice: BigInteger,
    ): TransactionFee.Choosable {
        val minFee = gasPrice * gasLimit
        //By dividing by ten before last multiplication here we can lose some digits
        val normalFee = gasPrice * BigInteger.valueOf(12) / BigInteger.TEN * gasLimit
        val priorityFee = gasPrice * BigInteger.valueOf(15) / BigInteger.TEN * gasLimit

        return TransactionFee.Choosable(
            minimum = Fee.Ethereum(
                amount = createFee(amountParams, minFee),
                gasLimit = gasLimit,
                gasPrice = gasPrice
            ),
            normal = Fee.Ethereum(
                amount = createFee(amountParams, normalFee),
                gasLimit = gasLimit,
                gasPrice = gasPrice
            ),
            priority = Fee.Ethereum(
                amount = createFee(amountParams, priorityFee),
                gasLimit = gasLimit,
                gasPrice = gasPrice
            )
        )
    }

    internal fun calculateFees(
        amountParams: Amount,
        gasLimit: BigInteger,
        gasPrice: BigInteger,
    ): TransactionFee.Choosable {
        val gasPriceDecimal = BigDecimal(gasPrice)
        val gasLimitDecimal = BigDecimal(gasLimit)

        val minFee = gasPriceDecimal * minimalMultiplier * gasLimitDecimal
        val normalFee = gasPriceDecimal * normalMultiplier * gasLimitDecimal
        val priorityFee = gasPriceDecimal * priorityMultiplier * gasLimitDecimal

        val minimalFeeBigInt = minFee.toBigInteger()
        val normalFeeBigInt = normalFee.toBigInteger()
        val priorityFeeBigInt = priorityFee.toBigInteger()

        return TransactionFee.Choosable(
            minimum = Fee.Ethereum(
                amount = createFee(amountParams, minimalFeeBigInt),
                gasLimit = gasLimit,
                gasPrice = gasPrice
            ),
            normal = Fee.Ethereum(
                amount = createFee(amountParams, normalFeeBigInt),
                gasLimit = gasLimit,
                gasPrice = gasPrice
            ),
            priority = Fee.Ethereum(
                amount = createFee(amountParams, priorityFeeBigInt),
                gasLimit = gasLimit,
                gasPrice = gasPrice
            )
        )
    }

    internal fun calculateSingleFee(
        amountParams: Amount,
        gasLimit: BigInteger,
        gasPrice: BigInteger,
    ): TransactionFee.Single {
        val gasPriceDecimal = BigDecimal(gasPrice)
        val gasLimitDecimal = BigDecimal(gasLimit)

        val normalFeeBigInt = (gasLimitDecimal * gasPriceDecimal).toBigInteger()

        return TransactionFee.Single(
            normal = Fee.Ethereum(
                amount = createFee(amountParams, normalFeeBigInt),
                gasLimit = gasLimit,
                gasPrice = gasPrice
            )
        )
    }

    private fun createFee(amountParams: Amount, value: BigInteger): Amount {
        return Amount(
            amountParams, value.toBigDecimal(
                scale = Blockchain.Ethereum.decimals(),
                mathContext = MathContext(Blockchain.Ethereum.decimals(), RoundingMode.HALF_EVEN)
            )
        )
    }

    companion object {

        val minimalMultiplier: BigDecimal = BigDecimal.valueOf(1)

        val normalMultiplier: BigDecimal =
            BigDecimal.valueOf(12).divide(BigDecimal.TEN, Blockchain.Ethereum.decimals(), RoundingMode.HALF_UP)

        val priorityMultiplier: BigDecimal =
            BigDecimal.valueOf(15).divide(BigDecimal.TEN, Blockchain.Ethereum.decimals(), RoundingMode.HALF_UP)
    }
}