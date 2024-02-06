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

    internal fun calculateFees(
        amountParams: Amount,
        gasLimit: BigInteger,
        gasPrice: BigInteger,
    ): TransactionFee.Choosable {
        val gasPriceDecimal = BigDecimal(gasPrice)
        val gasLimitDecimal = BigDecimal(gasLimit)

        val minGasPrice = gasPriceDecimal * minimalMultiplier
        val normalGasPrice = gasPriceDecimal * normalMultiplier
        val priorityGasPrice = gasPriceDecimal * priorityMultiplier

        val minFee = minGasPrice * gasLimitDecimal
        val normalFee = normalGasPrice * gasLimitDecimal
        val priorityFee = priorityGasPrice * gasLimitDecimal

        val minimalFeeBigInt = minFee.toBigInteger()
        val normalFeeBigInt = normalFee.toBigInteger()
        val priorityFeeBigInt = priorityFee.toBigInteger()

        return TransactionFee.Choosable(
            minimum = Fee.Ethereum(
                amount = createFee(amountParams, minimalFeeBigInt),
                gasLimit = gasLimit,
                gasPrice = minGasPrice.toBigInteger(),
            ),
            normal = Fee.Ethereum(
                amount = createFee(amountParams, normalFeeBigInt),
                gasLimit = gasLimit,
                gasPrice = normalGasPrice.toBigInteger(),
            ),
            priority = Fee.Ethereum(
                amount = createFee(amountParams, priorityFeeBigInt),
                gasLimit = gasLimit,
                gasPrice = priorityGasPrice.toBigInteger(),
            ),
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
                gasPrice = gasPrice,
            ),
        )
    }

    private fun createFee(amountParams: Amount, value: BigInteger): Amount {
        return Amount(
            amountParams,
            value.toBigDecimal(
                scale = Blockchain.Ethereum.decimals(),
                mathContext = MathContext(Blockchain.Ethereum.decimals(), RoundingMode.HALF_EVEN),
            ),
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
