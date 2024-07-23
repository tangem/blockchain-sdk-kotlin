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
    fun unmarshalSignatureEVMLegacy(signature: ByteArray, hash: ByteArray, publicKey: Wallet.PublicKey): ByteArray {
        val extendedSignature = unmarshalSignatureExtended(signature, hash, publicKey)
        val v = (extendedSignature.recId + 27).toBigInteger()
        val signatureData = SignatureData(extendedSignature.r, extendedSignature.s, v)

        return signatureData.r.toByteArray().removeLeadingZero() +
            signatureData.s.toByteArray().removeLeadingZero() +
            signatureData.v.toByteArray().removeLeadingZero()
    }

    @Suppress("MagicNumber")
    fun unmarshalSignatureExtended(
        signature: ByteArray,
        hash: ByteArray,
        publicKey: Wallet.PublicKey,
    ): ExtendedSecp256k1Signature {
        val r = BigInteger(1, signature.copyOfRange(0, 32))
        val s = BigInteger(1, signature.copyOfRange(32, 64))

        val ecdsaSignature = ECDSASignature(r, s).canonicalise()

        val recId = ecdsaSignature.determineRecId(
            messageHash = hash,
            publicKey = org.kethereum.model.PublicKey(
                publicKey.blockchainKey.toDecompressedPublicKey()
                    .sliceArray(1..64),
            ),
        )

        return ExtendedSecp256k1Signature(
            r = ecdsaSignature.r,
            s = ecdsaSignature.s,
            recId = recId,
        )
    }
}

data class ExtendedSecp256k1Signature(
    val r: BigInteger,
    val s: BigInteger,
    val recId: Int,
)