package com.tangem.blockchain.common

import com.tangem.common.extensions.toDecompressedPublicKey
import org.kethereum.crypto.api.ec.ECDSASignature
import org.kethereum.crypto.determineRecId
import org.kethereum.crypto.impl.ec.canonicalise
import org.kethereum.extensions.removeLeadingZero
import org.kethereum.model.SignatureData
import java.math.BigInteger

internal class UnmarshalHelper {

    @Suppress("MagicNumber")
    fun unmarshalSignature(signature: ByteArray, hash: ByteArray, publicKey: Wallet.PublicKey): ByteArray {
        val r = BigInteger(1, signature.copyOfRange(0, 32))
        val s = BigInteger(1, signature.copyOfRange(32, 64))

        val ecdsaSignature = ECDSASignature(r, s).canonicalise()

        val recId = ecdsaSignature.determineRecId(
            hash,
            org.kethereum.model.PublicKey(publicKey.blockchainKey.toDecompressedPublicKey().sliceArray(1..64)),
        )
        val v = (recId + 27).toBigInteger()
        val signatureData = SignatureData(ecdsaSignature.r, ecdsaSignature.s, v)

        return signatureData.r.toByteArray().removeLeadingZero() +
            signatureData.s.toByteArray().removeLeadingZero() +
            signatureData.v.toByteArray().removeLeadingZero()
    }
}
