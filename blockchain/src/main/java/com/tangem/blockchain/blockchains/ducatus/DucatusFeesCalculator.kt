package com.tangem.blockchain.blockchains.ducatus

import com.tangem.blockchain.blockchains.bitcoin.BitcoinFeesCalculator
import com.tangem.blockchain.common.Blockchain

internal class DucatusFeesCalculator(blockchain: Blockchain) : BitcoinFeesCalculator(blockchain) {

    override val minimalFeePerKb = 0.0001.toBigDecimal()
    override val minimalFee = 0.00001.toBigDecimal()
}