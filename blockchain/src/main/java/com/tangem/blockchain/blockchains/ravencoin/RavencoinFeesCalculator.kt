package com.tangem.blockchain.blockchains.ravencoin

import com.tangem.blockchain.blockchains.bitcoin.BitcoinFeesCalculator
import com.tangem.blockchain.common.Blockchain
import java.math.BigDecimal

internal class RavencoinFeesCalculator(blockchain: Blockchain) : BitcoinFeesCalculator(blockchain) {
    override val minimalFeePerKb: BigDecimal = BigDecimal(10_000).movePointLeft(blockchain.decimals())
}