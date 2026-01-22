package com.tangem.blockchain.common

import com.tangem.blockchain.extensions.normalizeByteArray
import com.tangem.common.extensions.toDecompressedPublicKey
import org.kethereum.crypto.api.ec.ECDSASignature
import org.kethereum.crypto.determineRecId
import org.kethereum.crypto.impl.ec.canonicalise
import org.kethereum.model.PublicKey
import org.kethereum.model.SignatureData
import java.math.BigInteger

object UnmarshalHelper {

    // R
    private const val COMPRESSED_CURVE_POINT_START_INDEX = 0
    private const val COMPRESSED_CURVE_POINT_END_INDEX = 32

    // S
    private const val SCALAR_START_INDEX = 32
    private const val SCALAR_END_INDEX = 64

    internal const val EVM_LEGACY_REC_ID_OFFSET = 27

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
                    .sliceArray(1..publicKey.lastIndex),
            ),
        )
        return ExtendedSecp256k1Signature(
            r = ecdsaSignature.r,
            s = ecdsaSignature.s,
            recId = recId,
        )
    }
}

data class ExtendedSecp256k1Signature(val r: BigInteger, val s: BigInteger, val recId: Int) {

    fun asRSVLegacyEVM(): ByteArray {
        return asRSV(UnmarshalHelper.EVM_LEGACY_REC_ID_OFFSET)
    }

    fun asRSV(recIdOffset: Int = 0): ByteArray {
        val v = recId + recIdOffset
        val signatureData = SignatureData(r = r, s = s, v = v.toBigInteger())

        // !!Important, use removeLeadingZero() from com.tangem.blockchain.extensions
        return signatureData.r.toByteArray().normalizeByteArray(size = 32) +
            signatureData.s.toByteArray().normalizeByteArray(size = 32) +
            signatureData.v.toByteArray().normalizeByteArray(size = 1)
    }
}