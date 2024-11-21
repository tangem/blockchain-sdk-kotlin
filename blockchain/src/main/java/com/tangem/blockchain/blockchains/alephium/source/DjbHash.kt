package com.tangem.blockchain.blockchains.alephium.source

@Suppress("MagicNumber")
internal object DjbHash {
    fun intHash(bytes: ByteArray): Int {
        var hash = 5381
        for (byte in bytes) {
            hash = (hash shl 5) + hash + (byte.toInt() and 0xff)
        }
        return hash
    }
}