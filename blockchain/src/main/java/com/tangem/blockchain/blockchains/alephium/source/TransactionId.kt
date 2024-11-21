package com.tangem.blockchain.blockchains.alephium.source

import kotlinx.io.bytestring.ByteString

internal data class TransactionId constructor(val value: Blake2b) {

    fun bytes(): ByteArray {
        return value.bytes()
    }

    companion object {
        fun hash(bytes: ByteString): TransactionId = TransactionId(Blake2bUtils.hash(bytes))
    }
}