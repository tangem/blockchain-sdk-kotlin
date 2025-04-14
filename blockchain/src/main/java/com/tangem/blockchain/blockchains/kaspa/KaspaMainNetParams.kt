package com.tangem.blockchain.blockchains.kaspa

import org.bitcoinj.core.Coin
import org.bitcoinj.core.Coin.COIN
import org.bitcoinj.params.MainNetParams

class KaspaMainNetParams : MainNetParams() {

    override fun getMaxMoney(): Coin {
        return COIN.multiply(MAX_KASPA_COINS)
    }

    private companion object {
        // Value from https://kaspa.org/tokenomics/
        const val MAX_KASPA_COINS = 28_700_000_000
    }
}