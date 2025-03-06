package com.tangem.blockchain.blockchains.alephium.source

import kotlinx.io.bytestring.ByteString
import kotlin.experimental.xor

@Suppress("MagicNumber", "UnnecessaryParentheses")
internal object Bytes {

    fun from(value: Int): ByteString = ByteString(
        byteArrayOf(
            (value shr 24).toByte(),
            (value shr 16).toByte(),
            (value shr 8).toByte(),
            value.toByte(),
        ),
    )

    fun toIntUnsafe(bytes: ByteString): Int {
        require(bytes.size == 4) { "Byte array must have exactly 4 bytes" }
        return (bytes[0].toInt() shl 24) or
            ((bytes[1].toInt() and 0xff) shl 16) or
            ((bytes[2].toInt() and 0xff) shl 8) or
            (bytes[3].toInt() and 0xff)
    }

    fun from(value: Long): ByteString = ByteString(
        byteArrayOf(
            (value shr 56).toByte(),
            (value shr 48).toByte(),
            (value shr 40).toByte(),
            (value shr 32).toByte(),
            (value shr 24).toByte(),
            (value shr 16).toByte(),
            (value shr 8).toByte(),
            value.toByte(),
        ),
    )

    fun toLongUnsafe(bytes: ByteString): Long {
        require(bytes.size == 8) { "Byte array must have exactly 8 bytes" }
        return ((bytes[0].toLong() and 0xff) shl 56) or
            ((bytes[1].toLong() and 0xff) shl 48) or
            ((bytes[2].toLong() and 0xff) shl 40) or
            ((bytes[3].toLong() and 0xff) shl 32) or
            ((bytes[4].toLong() and 0xff) shl 24) or
            ((bytes[5].toLong() and 0xff) shl 16) or
            ((bytes[6].toLong() and 0xff) shl 8) or
            (bytes[7].toLong() and 0xff)
    }

    fun xorByte(value: Int): Byte {
        val byte0 = (value shr 24).toByte()
        val byte1 = (value shr 16).toByte()
        val byte2 = (value shr 8).toByte()
        val byte3 = value.toByte()
        return (byte0 xor byte1 xor byte2 xor byte3).toByte()
    }
}