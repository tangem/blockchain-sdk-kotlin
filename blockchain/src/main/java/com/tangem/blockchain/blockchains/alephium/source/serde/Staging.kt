package com.tangem.blockchain.blockchains.alephium.source.serde

import kotlinx.io.bytestring.ByteString

internal data class Staging<out T>(val value: T, val rest: ByteString) {
    fun <B> mapValue(transform: (T) -> B): Staging<B> {
        return Staging(transform(value), rest)
    }
}