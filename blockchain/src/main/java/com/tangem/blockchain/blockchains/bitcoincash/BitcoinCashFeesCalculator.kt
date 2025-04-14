package com.tangem.blockchain.blockchains.bitcoincash

import com.tangem.blockchain.blockchains.bitcoin.BitcoinFeesCalculator
import com.tangem.blockchain.common.Blockchain

internal class BitcoinCashFeesCalculator(blockchain: Blockchain) : BitcoinFeesCalculator(blockchain) {
    override val minimalFee = 0.00001.toBigDecimal()
}