package com.tangem.blockchain.extensions

import com.tangem.blockchain.blockchains.binance.client.encoding.Bech32
import com.tangem.blockchain.blockchains.binance.client.encoding.Crypto
import org.bitcoinj.core.Base58

fun String.decodeBase58(): ByteArray? {
    return try {
        Base58.decode(this)
    } catch (exception: Exception) {
        null
    }
}

fun String.decodeBech32(): ByteArray? {
    return try {
        val decoded: ByteArray = Bech32.decode(this).data
        Crypto.convertBits(decoded, 0, decoded.size, 5, 8, false)
    } catch (exception: Exception) {
        null
    }
}