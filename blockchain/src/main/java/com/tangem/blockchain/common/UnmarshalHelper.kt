package com.tangem.blockchain.common

import com.tangem.common.extensions.toDecompressedPublicKey
import org.kethereum.crypto.api.ec.ECDSASignature
import org.kethereum.crypto.determineRecId
import org.kethereum.crypto.impl.ec.canonicalise
import org.kethereum.extensions.removeLeadingZero
import org.kethereum.model.PublicKey
import org.kethereum.model.SignatureData
import java.math.BigInteger

internal class UnmarshalHelper {

    fun unmarshalSignatureEVMLegacy(signature: ByteArray, hash: ByteArray, publicKey: Wallet.PublicKey): ByteArray {
        return unmarshalSignatureExtended(signature, hash, publicKey).asRSV(recIdOffset = EVM_LEGACY_REC_ID_OFFSET)
    }

    fun unmarshalSignatureExtended(
        signature: ByteArray,
        hash: ByteArray,
        publicKey: Wallet.PublicKey,
    ): ExtendedSecp256k1Signature {
        return unmarshalSignatureExtended(
            signature = signature,
            hash = hash,
            publicKey = publicKey.blockchainKey.toDecompressedPublicKey(),
        )
    }

    fun unmarshalSignatureExtended(
        signature: ByteArray,
        hash: ByteArray,
        publicKey: ByteArray,
    ): ExtendedSecp256k1Signature {
        val r = BigInteger(
            1,
            signature.copyOfRange(
                fromIndex = COMPRESSED_CURVE_POINT_START_INDEX,
                toIndex = COMPRESSED_CURVE_POINT_END_INDEX,
            ),
        )

        val s = BigInteger(1, signature.copyOfRange(SCALAR_START_INDEX, SCALAR_END_INDEX))
        val ecdsaSignature = ECDSASignature(r, s).canonicalise()

        val recId = ecdsaSignature.determineRecId(
            messageHash = hash,
            publicKey = PublicKey(
                publicKey.toDecompressedPublicKey()
                    .sliceArray(1..PUBLIC_KEY_SIZE),
            ),
        )
        return ExtendedSecp256k1Signature(
            r = ecdsaSignature.r,
            s = ecdsaSignature.s,
            recId = recId,
        )
    }

    companion object {
        private const val PUBLIC_KEY_SIZE = 64

        // R
        private const val COMPRESSED_CURVE_POINT_START_INDEX = 0
        private const val COMPRESSED_CURVE_POINT_END_INDEX = 32

        // S
        private const val SCALAR_START_INDEX = 32
        private const val SCALAR_END_INDEX = 64

        const val EVM_LEGACY_REC_ID_OFFSET = 27
    }
}

internal data class ExtendedSecp256k1Signature(val r: BigInteger, val s: BigInteger, val recId: Int) {

    fun asRSV(recIdOffset: Int = 0): ByteArray {
        val v = recId + recIdOffset
        val signatureData = SignatureData(r = r, s = s, v = v.toBigInteger())

        val partV = if (v == 0) {
            signatureData.v.toByteArray()
        } else {
            signatureData.v.toByteArray().removeLeadingZero()
        }
        return signatureData.r.toByteArray().removeLeadingZero() +
            signatureData.s.toByteArray().removeLeadingZero() +
            partV
    }
}