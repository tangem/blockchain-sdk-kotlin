package com.tangem.blockchain.blockchains.bitcoin

import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Calculates and returns bitcoin fees
 */
internal open class BitcoinFeesCalculator(
    private val blockchain: Blockchain,
) {

    open val minimalFeePerKb = BitcoinWalletManager.DEFAULT_MINIMAL_FEE_PER_KB.toBigDecimal()
    open val minimalFee = 0.000001.toBigDecimal()

    fun calculateFees(transactionSize: BigDecimal, feeResult: BitcoinFee): TransactionFee.Choosable {
        val minFee = calculateFee(feeResult.minimalPerKb, transactionSize)
        val normalFee = calculateFee(feeResult.normalPerKb, transactionSize)
        val priorityFee = calculateFee(feeResult.priorityPerKb, transactionSize)
        return TransactionFee.Choosable(
            minimum = minFee,
            normal = normalFee,
            priority = priorityFee,
        )
    }

    private fun calculateFee(feePerKb: BigDecimal, transactionSize: BigDecimal): Fee.Bitcoin {
        val innerFeePerKb = maxOf(a = feePerKb, b = minimalFeePerKb)
        // Rounding up to integer
        val satoshiPerByte = innerFeePerKb
            .divide(BigDecimal(BYTES_IN_KB))
            .movePointRight(blockchain.decimals())
            .setScale(INT_DECIMALS, RoundingMode.UP)
        val calculatedFee = satoshiPerByte
            .movePointLeft(blockchain.decimals())
            .multiply(transactionSize)
            .setScale(blockchain.decimals(), RoundingMode.UP)

        val fee = maxOf(calculatedFee, minimalFee)
        return Fee.Bitcoin(Amount(fee, blockchain), satoshiPerByte, transactionSize)
    }

    private companion object {
        const val BYTES_IN_KB = 1000 // use 1000 for BTC
        const val INT_DECIMALS = 0
    }
}