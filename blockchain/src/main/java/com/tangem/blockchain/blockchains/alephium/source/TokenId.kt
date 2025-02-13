package com.tangem.blockchain.blockchains.alephium.source

import com.tangem.blockchain.blockchains.alephium.source.serde.Serde
import com.tangem.blockchain.blockchains.alephium.source.serde.Staging
import kotlinx.io.bytestring.ByteString

@JvmInline
internal value class TokenId constructor(val value: Blake2b256) {
    fun length(): Int {
        return value.length()
    }

    fun bytes(): ByteArray {
        return value.bytes()
    }

    companion object {
        val serde = object : Serde<TokenId> {
            override fun serialize(input: TokenId): ByteString = Blake2b256.serde.serialize(input.value)
            override fun _deserialize(input: ByteString): Result<Staging<TokenId>> =
                Blake2b256.serde._deserialize(input).map { it.mapValue { value -> TokenId(value) } }
        }
    }
}