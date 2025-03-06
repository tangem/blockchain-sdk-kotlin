package com.tangem.blockchain.blockchains.alephium.source

import com.tangem.blockchain.blockchains.alephium.source.serde.byteArraySerde
import kotlinx.io.bytestring.ByteString
import org.bouncycastle.crypto.Digest
import org.bouncycastle.crypto.digests.Blake2bDigest

internal data class Blake2b256(val bytes: ByteString) {

    fun length(): Int {
        return bytes.size
    }

    fun bytes(): ByteArray {
        return bytes.toByteArray()
    }

    companion object {
        val serde = byteArraySerde(Blake2b256Utils.length)
            .xmap(to = { Blake2b256(it) }, from = { it.bytes })
    }
}

@Suppress("MagicNumber")
internal object Blake2b256Utils {

    const val length: Int = 32

    fun provider(): Digest = Blake2bDigest(length * 8)

    fun hash(input: ByteString): Blake2b256 {
        val hashser = provider()
        hashser.update(input.toByteArray(), 0, input.size)
        val res = ByteArray(length)
        hashser.doFinal(res, 0)
        return Blake2b256(ByteString(res))
    }
}