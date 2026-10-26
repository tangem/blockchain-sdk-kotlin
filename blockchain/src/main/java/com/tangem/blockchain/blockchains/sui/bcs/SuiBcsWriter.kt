package com.tangem.blockchain.blockchains.sui.bcs

import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.common.extensions.hexToBytes
import java.io.ByteArrayOutputStream

/** Binary Canonical Serialization as used by Sui: little-endian integers, ULEB128 lengths and enum tags */
internal class SuiBcsWriter {

    private val output = ByteArrayOutputStream()

    fun toByteArray(): ByteArray = output.toByteArray()

    fun u8(value: Int) = apply { output.write(value) }

    fun u16(value: Int) = apply { littleEndian(value.toLong(), byteCount = 2) }

    fun u32(value: Long) = apply { littleEndian(value, byteCount = 4) }

    fun u64(value: Long) = apply { littleEndian(value, byteCount = 8) }

    fun uleb128(value: Int) = apply {
        var remaining = value
        while (remaining >= ULEB_CONTINUATION) {
            output.write(remaining and ULEB_PAYLOAD or ULEB_CONTINUATION)
            remaining = remaining ushr ULEB_PAYLOAD_BITS
        }
        output.write(remaining)
    }

    fun bytes(value: ByteArray) = apply {
        uleb128(value.size)
        output.write(value)
    }

    fun string(value: String) = bytes(value.toByteArray(Charsets.UTF_8))

    /** 32-byte account address or object id, written without a length prefix */
    fun address(value: String) = apply {
        val raw = value.removePrefix(HEX_PREFIX).padStart(length = ADDRESS_LENGTH * 2, padChar = '0')
        val bytes = raw.hexToBytes()
        require(bytes.size == ADDRESS_LENGTH) { "Sui address must be $ADDRESS_LENGTH bytes: $value" }
        output.write(bytes)
    }

    /** Sui digests are 32 bytes serialized as a length-prefixed byte array */
    fun digest(base58: String) = apply {
        val bytes = requireNotNull(base58.decodeBase58()) { "Invalid Sui digest: $base58" }
        require(bytes.size == DIGEST_LENGTH) { "Sui digest must be $DIGEST_LENGTH bytes: $base58" }
        bytes(bytes)
    }

    fun optionU64(value: Long?) = apply {
        if (value == null) {
            u8(0)
        } else {
            u8(1).u64(value)
        }
    }

    fun <T> vector(items: List<T>, writeItem: SuiBcsWriter.(T) -> Unit) = apply {
        uleb128(items.size)
        items.forEach { writeItem(it) }
    }

    private fun littleEndian(value: Long, byteCount: Int) {
        repeat(byteCount) { index -> output.write((value ushr index * Byte.SIZE_BITS).toInt() and BYTE_MASK) }
    }

    private companion object {
        const val HEX_PREFIX = "0x"
        const val ADDRESS_LENGTH = 32
        const val DIGEST_LENGTH = 32
        const val BYTE_MASK = 0xFF
        const val ULEB_CONTINUATION = 0x80
        const val ULEB_PAYLOAD = 0x7F
        const val ULEB_PAYLOAD_BITS = 7
    }
}