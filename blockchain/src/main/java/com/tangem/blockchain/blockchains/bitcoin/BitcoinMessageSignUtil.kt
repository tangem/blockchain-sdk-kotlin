package com.tangem.blockchain.blockchains.bitcoin

import com.tangem.blockchain.extensions.normalizeByteArray
import com.tangem.blockchain.extensions.toCanonicalECDSASignature
import com.tangem.common.extensions.calculateSha256
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.VarInt
import java.io.ByteArrayOutputStream

object BitcoinMessageSignUtil {

    const val BITCOIN_MESSAGE_MAGIC = "\u0018Bitcoin Signed Message:\n"
    const val CLORE_MESSAGE_MAGIC = "\u0016Clore Signed Message:\n"

    fun createMessageHash(message: String, messageMagic: String): ByteArray {
        val messageBytes = message.toByteArray(Charsets.UTF_8)
        val magicBytes = messageMagic.toByteArray(Charsets.UTF_8)

        val outputStream = ByteArrayOutputStream()
        outputStream.write(magicBytes)
        outputStream.write(VarInt(messageBytes.size.toLong()).encode())
        outputStream.write(messageBytes)

        val fullMessage = outputStream.toByteArray()
        return fullMessage.calculateSha256().calculateSha256()
    }

    @Suppress("MagicNumber")
    fun createRecoverableSignature(
        signatureBytes: ByteArray,
        publicKey: ByteArray,
        messageHash: ByteArray,
    ): ByteArray {
        val ecdsaSignature = signatureBytes.toCanonicalECDSASignature()
        val canonicalR = ecdsaSignature.r
        val canonicalS = ecdsaSignature.s

        val ecKey = ECKey.fromPublicOnly(publicKey)
        val isCompressed = publicKey.size == 33

        var recoveryId = -1
        for (i in 0..3) {
            val recoveredKey = ECKey.recoverFromSignature(
                i,
                ecdsaSignature,
                org.bitcoinj.core.Sha256Hash.wrap(messageHash),
                isCompressed,
            )
            if (recoveredKey != null && recoveredKey.pubKeyPoint == ecKey.pubKeyPoint) {
                recoveryId = i
                break
            }
        }

        require(recoveryId >= 0) { "Could not find recovery ID for signature" }

        val headerByte = if (isCompressed) {
            (31 + recoveryId).toByte()
        } else {
            (27 + recoveryId).toByte()
        }

        val result = ByteArray(65)
        result[0] = headerByte
        System.arraycopy(canonicalR.toByteArray().normalizeByteArray(32), 0, result, 1, 32)
        System.arraycopy(canonicalS.toByteArray().normalizeByteArray(32), 0, result, 33, 32)

        return result
    }
}