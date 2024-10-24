package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.blockchains.ethereum.network.EthereumFeeHistory
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
        val gasLimitDecimal = BigDecimal(gasLimit)

        val (minGasPrice, normalGasPrice, priorityGasPrice) = gasPrice.calculateByPriority()

        val minFee = minGasPrice * gasLimitDecimal
        val normalFee = normalGasPrice * gasLimitDecimal
        val priorityFee = priorityGasPrice * gasLimitDecimal

        val minimalFeeBigInt = minFee.toBigInteger()
        val normalFeeBigInt = normalFee.toBigInteger()
        val priorityFeeBigInt = priorityFee.toBigInteger()

        return TransactionFee.Choosable(
            minimum = Fee.Ethereum.Legacy(
                amount = createFee(amountParams, minimalFeeBigInt),
                gasLimit = gasLimit,
                gasPrice = minGasPrice.toBigInteger(),
            ),
            normal = Fee.Ethereum.Legacy(
                amount = createFee(amountParams, normalFeeBigInt),
                gasLimit = gasLimit,
                gasPrice = normalGasPrice.toBigInteger(),
            ),
            priority = Fee.Ethereum.Legacy(
                amount = createFee(amountParams, priorityFeeBigInt),
                gasLimit = gasLimit,
                gasPrice = priorityGasPrice.toBigInteger(),
            ),
        )
    }

    internal fun calculateEip1559Fees(
        amountParams: Amount,
        gasLimit: BigInteger,
        feeHistory: EthereumFeeHistory,
    ): TransactionFee.Choosable {
        val gasLimitDecimal = BigDecimal(gasLimit)

        val (lowPriorityFee, marketPriorityFee, fastPriorityFee) = when (feeHistory) {
            is EthereumFeeHistory.Common -> feeHistory.toTriple()
            is EthereumFeeHistory.Fallback -> feeHistory.gasPrice.calculateByPriority()
        }

        val minMaxFeePerGas = feeHistory.baseFee * minimalMultiplier + lowPriorityFee
        val normalMaxFeePerGas = feeHistory.baseFee * normalMultiplier + marketPriorityFee
        val priorityMaxFeePerGas = feeHistory.baseFee * priorityMultiplier + fastPriorityFee

        val minFee = minMaxFeePerGas * gasLimitDecimal
        val normalFee = normalMaxFeePerGas * gasLimitDecimal
        val priorityFee = priorityMaxFeePerGas * gasLimitDecimal

        val minimalFeeBigInt = minFee.toBigInteger()
        val normalFeeBigInt = normalFee.toBigInteger()
        val priorityFeeBigInt = priorityFee.toBigInteger()

        return TransactionFee.Choosable(
            minimum = Fee.Ethereum.EIP1559(
                amount = createFee(amountParams, minimalFeeBigInt),
                gasLimit = gasLimit,
                maxFeePerGas = minMaxFeePerGas.toBigInteger(),
                priorityFee = lowPriorityFee.toBigInteger(),
            ),
            normal = Fee.Ethereum.EIP1559(
                amount = createFee(amountParams, normalFeeBigInt),
                gasLimit = gasLimit,
                maxFeePerGas = normalMaxFeePerGas.toBigInteger(),
                priorityFee = marketPriorityFee.toBigInteger(),
            ),
            priority = Fee.Ethereum.EIP1559(
                amount = createFee(amountParams, priorityFeeBigInt),
                gasLimit = gasLimit,
                maxFeePerGas = priorityMaxFeePerGas.toBigInteger(),
                priorityFee = fastPriorityFee.toBigInteger(),
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
            normal = Fee.Ethereum.Legacy(
                amount = createFee(amountParams, normalFeeBigInt),
                gasLimit = gasLimit,
                gasPrice = gasPrice,
            ),
        )
    }

    internal fun calculateEip1559SingleFee(
        amountParams: Amount,
        gasLimit: BigInteger,
        feeHistory: EthereumFeeHistory,
    ): TransactionFee.Single {
        val gasLimitDecimal = BigDecimal(gasLimit)

        val marketPriorityFee = when (feeHistory) {
            is EthereumFeeHistory.Common -> feeHistory.marketPriorityFee
            is EthereumFeeHistory.Fallback -> BigDecimal(feeHistory.gasPrice) * normalMultiplier
        }

        val normalMaxFeePerGas = feeHistory.baseFee * normalMultiplier + marketPriorityFee

        val normalFee = normalMaxFeePerGas * gasLimitDecimal

        return TransactionFee.Single(
            normal = Fee.Ethereum.EIP1559(
                amount = createFee(amountParams, normalFee.toBigInteger()),
                gasLimit = gasLimit,
                maxFeePerGas = normalMaxFeePerGas.toBigInteger(),
                priorityFee = marketPriorityFee.toBigInteger(),
            ),
        )
    }

    private fun BigInteger.calculateByPriority(): Triple<BigDecimal, BigDecimal, BigDecimal> {
        val gasPriceDecimal = BigDecimal(this)

        val minGasPrice = gasPriceDecimal * minimalMultiplier
        val normalGasPrice = gasPriceDecimal * normalMultiplier
        val priorityGasPrice = gasPriceDecimal * priorityMultiplier

        return Triple(first = minGasPrice, second = normalGasPrice, third = priorityGasPrice)
    }

    private fun createFee(amountParams: Amount, value: BigInteger): Amount {
        return Amount(
            amount = amountParams,
            value = value.toBigDecimal(
                scale = Blockchain.Ethereum.decimals(),
                mathContext = MathContext(Blockchain.Ethereum.decimals(), RoundingMode.HALF_EVEN),
            ),
        )
    }

    private companion object {

        val minimalMultiplier: BigDecimal = BigDecimal.valueOf(1)

        val normalMultiplier: BigDecimal =
            BigDecimal.valueOf(12).divide(BigDecimal.TEN, Blockchain.Ethereum.decimals(), RoundingMode.HALF_UP)

        val priorityMultiplier: BigDecimal =
            BigDecimal.valueOf(15).divide(BigDecimal.TEN, Blockchain.Ethereum.decimals(), RoundingMode.HALF_UP)
    }
}
