package com.tangem.blockchain.blockchains.clore

import com.tangem.blockchain.blockchains.bitcoin.BitcoinFeesCalculator
import com.tangem.blockchain.common.Blockchain
import java.math.BigDecimal

internal class CloreFeesCalculator(blockchain: Blockchain) : BitcoinFeesCalculator(blockchain) {
    override val minimalFeePerKb: BigDecimal = BigDecimal(100_000).movePointLeft(blockchain.decimals())
    override val minimalFee = 0.01.toBigDecimal()
}