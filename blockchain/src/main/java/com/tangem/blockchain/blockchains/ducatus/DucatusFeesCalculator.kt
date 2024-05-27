package com.tangem.blockchain.blockchains.ducatus

import com.tangem.blockchain.blockchains.bitcoin.BitcoinFeesCalculator
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import java.math.BigDecimal

internal class DucatusFeesCalculator(val blockchain: Blockchain) : BitcoinFeesCalculator(blockchain) {

    override val minimalFee = 0.00001.toBigDecimal()

    fun calculateFees(transactionSize: BigDecimal): TransactionFee.Choosable {
        val minFee = minFee.multiply(transactionSize)
        val normalFee = normalFee.multiply(transactionSize)
        val priorityFee = priorityFee.multiply(transactionSize)
        return TransactionFee.Choosable(
            minimum = Fee.Common(Amount(minFee, blockchain)),
            normal = Fee.Common(Amount(normalFee, blockchain)),
            priority = Fee.Common(Amount(priorityFee, blockchain)),
        )
    }

    private companion object {
        val minFee = 0.00000089.toBigDecimal()
        val normalFee = 0.00000144.toBigDecimal()
        val priorityFee = 0.00000350.toBigDecimal()
    }
}