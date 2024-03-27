package com.tangem.blockchain.blockchains.nexa

import org.bitcoinj.core.Coin
import org.bitcoinj.params.MainNetParams

class NexaMainNetParams : MainNetParams() {


    override fun getMaxMoney(): Coin {
        return Coin.valueOf(MAX_NEXA_COINS)
    }

    private companion object {
        // Value from https://forum.nexa.org/t/nexa-tokenomics-are-modelled-for-utility/46
        const val MAX_NEXA_COINS = 2_100_000_000_000_000
    }
}