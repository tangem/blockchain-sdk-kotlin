package com.tangem.blockchain.blockchains.pepecoin

import org.bitcoinj.core.Coin
import org.bitcoinj.params.MainNetParams

@Suppress("MagicNumber")
internal class PepecoinMainNetParams : MainNetParams() {

    init {
        segwitAddressHrp = "P"
        addressHeader = 0x38
        p2shHeader = 0x16
        dumpedPrivateKeyHeader = 0x9E
        bip32HeaderP2PKHpub = 0x02FACAFD
        bip32HeaderP2PKHpriv = 0x02FAC398
    }

    override fun hasMaxMoney(): Boolean = true

    override fun getMaxMoney(): Coin = Coin.valueOf(Long.MAX_VALUE)
}