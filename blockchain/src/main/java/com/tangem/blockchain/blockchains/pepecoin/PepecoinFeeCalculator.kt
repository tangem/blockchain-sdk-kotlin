package com.tangem.blockchain.blockchains.pepecoin

import com.tangem.blockchain.blockchains.bitcoin.BitcoinFeesCalculator
import com.tangem.blockchain.common.Blockchain
import java.math.BigDecimal

internal open class PepecoinFeeCalculator(
    private val blockchain: Blockchain,
): BitcoinFeesCalculator(blockchain) {

    override val minimalFeePerKb: BigDecimal = 0.01.toBigDecimal()
    override val minimalFee: BigDecimal = 0.01.toBigDecimal()
}