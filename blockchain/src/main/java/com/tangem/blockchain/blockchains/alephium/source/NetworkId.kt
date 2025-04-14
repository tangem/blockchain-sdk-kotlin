package com.tangem.blockchain.blockchains.alephium.source

import com.tangem.blockchain.blockchains.alephium.source.serde.Serde

@JvmInline
internal value class NetworkId(val id: Byte = 0) {
    companion object {
        val serde = Serde.Companion.ByteSerde.xmap(::NetworkId) { it.id }
        val mainNet get() = NetworkId(0)
        val testNet get() = NetworkId(1)
    }
}