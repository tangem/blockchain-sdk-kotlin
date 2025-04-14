package com.tangem.blockchain.blockchains.alephium.source.serde

import kotlinx.io.bytestring.ByteString

internal fun interface Serializer<T> {
    fun serialize(input: T): ByteString
}