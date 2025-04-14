package com.tangem.blockchain.blockchains.alephium.source

import com.tangem.blockchain.blockchains.alephium.source.serde.Serde
import com.tangem.blockchain.blockchains.alephium.source.serde.Staging
import com.tangem.blockchain.blockchains.alephium.source.serde.intSerde
import kotlinx.io.bytestring.ByteString

@JvmInline
internal value class GasBox constructor(val value: Int) : Comparable<GasBox> {
    fun use(amount: GasBox): Result<GasBox> {
        return if (this >= amount) {
            Result.success(GasBox(value - amount.value))
        } else {
            Result.failure(RuntimeException("OutOfGas"))
        }
    }

    fun toU256(): U256 {
        return U256.unsafe(value.toBigDecimal())
    }

    override fun compareTo(other: GasBox): Int {
        return this.value.compareTo(other.value)
    }

    companion object {
        val serde = object : Serde<GasBox> {
            override fun serialize(input: GasBox): ByteString = intSerde.serialize(input.value)
            override fun _deserialize(input: ByteString): Result<Staging<GasBox>> =
                intSerde._deserialize(input).map { Staging(GasBox(it.value), it.rest) }
        }.validate { box ->
            if (box.value >= 0) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Negative gas ${box.value}"))
            }
        }

        fun unsafe(initialGas: Int): GasBox {
            require(initialGas >= 0)
            return GasBox(initialGas)
        }
    }
}