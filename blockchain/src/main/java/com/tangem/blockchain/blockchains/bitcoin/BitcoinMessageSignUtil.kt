package com.tangem.blockchain.blockchains.bitcoin

import com.tangem.common.extensions.calculateSha256
import org.bitcoinj.core.ECKey
import java.io.ByteArrayOutputStream
import java.math.BigInteger

object BitcoinMessageSignUtil {

    const val BITCOIN_MESSAGE_MAGIC = "\u0018Bitcoin Signed Message:\n"
    const val CLORE_MESSAGE_MAGIC = "\u0016Clore Signed Message:\n"

    fun createMessageHash(message: String, messageMagic: String): ByteArray {
        val messageBytes = message.toByteArray(Charsets.UTF_8)
        val magicBytes = messageMagic.toByteArray(Charsets.UTF_8)

        val outputStream = ByteArrayOutputStream()
        outputStream.write(magicBytes)
        writeVarInt(outputStream, messageBytes.size.toLong())
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
        require(signatureBytes.size == 64) { "Signature must be 64 bytes" }

        val r = BigInteger(1, signatureBytes.copyOfRange(0, 32))
        val s = BigInteger(1, signatureBytes.copyOfRange(32, 64))

        val ecdsaSignature = ECKey.ECDSASignature(r, s).toCanonicalised()
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
        System.arraycopy(canonicalR.toByteArray().padToLength(32), 0, result, 1, 32)
        System.arraycopy(canonicalS.toByteArray().padToLength(32), 0, result, 33, 32)

        return result
    }

    @Suppress("MagicNumber")
    private fun writeVarInt(stream: ByteArrayOutputStream, value: Long) {
        when {
            value < 0xFD -> {
                stream.write(value.toInt())
            }
            value <= 0xFFFF -> {
                stream.write(0xFD)
                stream.write((value and 0xFF).toInt())
                stream.write(((value shr 8) and 0xFF).toInt())
            }
            value <= 0xFFFFFFFF -> {
                stream.write(0xFE)
                stream.write((value and 0xFF).toInt())
                stream.write(((value shr 8) and 0xFF).toInt())
                stream.write(((value shr 16) and 0xFF).toInt())
                stream.write(((value shr 24) and 0xFF).toInt())
            }
            else -> {
                stream.write(0xFF)
                for (i in 0..7) {
                    stream.write(((value shr (i * 8)) and 0xFF).toInt())
                }
            }
        }
    }

    @Suppress("MagicNumber")
    private fun ByteArray.padToLength(length: Int): ByteArray {
        return when {
            this.size > length && this[0] == 0.toByte() -> {
                this.copyOfRange(this.size - length, this.size)
            }
            this.size > length -> {
                this.copyOfRange(this.size - length, this.size)
            }
            this.size < length -> {
                ByteArray(length - this.size) + this
            }
            else -> this
        }
    }
}