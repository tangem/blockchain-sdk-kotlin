package com.tangem.blockchain.blockchains.litecoin

import com.tangem.blockchain.blockchains.bitcoin.BitcoinFeesCalculator
import com.tangem.blockchain.common.Blockchain

internal class LitecoinFeesCalculator(blockchain: Blockchain) : BitcoinFeesCalculator(blockchain) {
    override val minimalFee = 0.00001.toBigDecimal()
}