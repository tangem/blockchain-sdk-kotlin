package com.tangem.blockchain.blockchains.pepecoin

import com.tangem.blockchain.blockchains.bitcoin.BitcoinFeesCalculator
import com.tangem.blockchain.common.Blockchain
import java.math.BigDecimal

internal open class PepecoinFeeCalculator(
    private val blockchain: Blockchain,
) : BitcoinFeesCalculator(blockchain) {

    // https://github.com/pepecoinppc/pepecoin/blob/4fb5a0cd930c0df82c88292e973a7b7cfa06c4e8/doc/fee-recommendation.md
    override val minimalFeePerKb: BigDecimal = 0.01.toBigDecimal()
    override val minimalFee: BigDecimal = 0.01.toBigDecimal()
}