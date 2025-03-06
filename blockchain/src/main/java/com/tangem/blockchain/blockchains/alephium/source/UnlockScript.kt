package com.tangem.blockchain.blockchains.alephium.source

import com.tangem.blockchain.blockchains.alephium.source.serde.*
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.append
import kotlinx.io.bytestring.buildByteString

internal sealed interface UnlockScript {

    @JvmInline
    value class P2PKH(
        val publicKey: ByteString,
    ) : UnlockScript {
        companion object {
            const val length: Int = 33
            val serde = byteArraySerde(length)
                .xmap(to = ::P2PKH, from = { it.publicKey })
        }
    }

    data object SameAsPrevious : UnlockScript

    companion object {
        val serde = object : Serde<UnlockScript> {
            override fun serialize(input: UnlockScript): ByteString = when (input) {
                is P2PKH -> buildByteString {
                    append(ByteString(0))
                    append(P2PKH.serde.serialize(input))
                }
                SameAsPrevious -> buildByteString { append(ByteString(3)) }
            }

            override fun _deserialize(input: ByteString): Result<Staging<UnlockScript>> {
                val deserialize = byteSerde._deserialize(input).getOrElse { return Result.failure(it) }
                return when (deserialize.value) {
                    0.toByte() -> UnlockScript.P2PKH.serde._deserialize(input)
                    3.toByte() -> Result.success(Staging(SameAsPrevious, input))
                    else -> Result.failure(SerdeError.wrongFormat("Invalid unlock script prefix ${deserialize.value}"))
                }
            }
        }
    }
}