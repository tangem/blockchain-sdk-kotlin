package com.tangem.blockchain.blockchains.pepecoin

import org.bitcoinj.params.MainNetParams

@Suppress("MagicNumber")
class PepecoinTestNetParams : MainNetParams() {
    init {
        segwitAddressHrp = "P"
        addressHeader = 0x71
        p2shHeader = 0xc4
        dumpedPrivateKeyHeader = 0xf1
        bip32HeaderP2PKHpub = 0x043587CF
        bip32HeaderP2PKHpriv = 0x04358394
    }
}