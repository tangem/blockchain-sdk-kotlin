package com.tangem.blockchain.blockchains.factorn

import org.bitcoinj.params.MainNetParams

internal class Fact0rnMainNetParams : MainNetParams() {

    init {
        segwitAddressHrp = "fact"
    }
}