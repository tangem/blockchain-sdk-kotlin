package com.tangem.blockchain.blockchains.litecoin

import org.libdohj.params.LitecoinMainNetParams

class LitecoinSegwitMainNetParams : LitecoinMainNetParams() {
    init {
        segwitAddressHrp = "ltc"
    }
}