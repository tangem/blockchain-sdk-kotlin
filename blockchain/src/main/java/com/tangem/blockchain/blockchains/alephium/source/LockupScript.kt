package com.tangem.blockchain.blockchains.alephium.source

import com.tangem.blockchain.blockchains.alephium.source.serde.Serde
import com.tangem.blockchain.blockchains.alephium.source.serde.SerdeError
import com.tangem.blockchain.blockchains.alephium.source.serde.Staging
import com.tangem.blockchain.blockchains.alephium.source.serde.byteSerde
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.append
import kotlinx.io.bytestring.buildByteString

internal sealed interface LockupScript {
    sealed interface Asset : LockupScript {
        val scriptHint: ScriptHint
    }

    data class P2PKH(
        val pkHash: Blake2b,
    ) : Asset {
        override val scriptHint: ScriptHint = ScriptHint.fromHash(pkHash)

        companion object {
            val serde = Blake2b.serde
        }
    }

    companion object {
        val serde = object : Serde<LockupScript.Asset> {
            override fun serialize(input: LockupScript.Asset): ByteString = when (input) {
                is P2PKH -> buildByteString {
                    append(ByteString(0))
                    append(P2PKH.serde.serialize(input.pkHash))
                }
            }

            override fun _deserialize(input: ByteString): Result<Staging<LockupScript.Asset>> {
                val deserialize = byteSerde._deserialize(input).getOrElse { return Result.failure(it) }
                return when (deserialize.value) {
                    0.toByte() -> P2PKH.serde._deserialize(input).map { it.mapValue { value -> P2PKH(value) } }
                    else -> Result.failure(SerdeError.wrongFormat("Invalid lockupScript prefix ${deserialize.value}"))
                }
            }
        }

        fun p2pkh(publicKey: ByteString): P2PKH {
            return P2PKH(Blake2bUtils.hash(publicKey))
        }
    }
}