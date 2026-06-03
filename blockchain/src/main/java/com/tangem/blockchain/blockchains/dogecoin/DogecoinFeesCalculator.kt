package com.tangem.blockchain.blockchains.dogecoin

import com.tangem.blockchain.blockchains.bitcoin.BitcoinFeesCalculator
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import java.math.BigDecimal

internal class DogecoinFeesCalculator(
    blockchain: Blockchain,
) : BitcoinFeesCalculator(blockchain) {

    override fun calculateFees(transactionSize: BigDecimal, feeResult: BitcoinFee): TransactionFee.Choosable {
        val baseFees = super.calculateFees(transactionSize, feeResult)

        return TransactionFee.Choosable(
            minimum = (baseFees.minimum as Fee.Bitcoin).coerceAtLeast(MINIMAL_SLOW_FEE),
            normal = (baseFees.normal as Fee.Bitcoin).coerceAtLeast(MINIMAL_MARKET_FEE),
            priority = (baseFees.priority as Fee.Bitcoin).coerceAtLeast(MINIMAL_FAST_FEE),
        )
    }

    private fun Fee.Bitcoin.coerceAtLeast(minFee: BigDecimal): Fee.Bitcoin {
        val currentValue = amount.value ?: return this
        return if (currentValue < minFee) {
            copy(amount = amount.copy(value = minFee))
        } else {
            this
        }
    }

    private companion object {
        val MINIMAL_SLOW_FEE = BigDecimal("0.01")
        val MINIMAL_MARKET_FEE = BigDecimal("0.1")
        val MINIMAL_FAST_FEE = BigDecimal("1")
    }
}