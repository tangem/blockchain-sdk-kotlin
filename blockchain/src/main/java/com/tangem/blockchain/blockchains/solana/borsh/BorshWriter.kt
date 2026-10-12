package com.tangem.blockchain.blockchains.solana.borsh

import org.p2p.solanaj.core.PublicKey
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Minimal borsh encoder covering the types Anchor programs use for their instruction arguments.
 *
 * Borsh is not self-describing: values are written back to back with no field names, tags or padding, always
 * little-endian. The reader has to know the layout, so argument order must follow the IDL exactly.
 *
 * Deliberately separate from the kotlinx-serialization encoder in `alt/borsh`: that one is built on
 * kotlinx-serialization, keeps `encodeBytes` private (a raw 32-byte pubkey would have to be written byte by byte) and
 * strips null bytes when decoding strings.
 */
internal class BorshWriter {

    private val buffer = ByteArrayOutputStream()

    fun u8(value: Int): BorshWriter {
        buffer.write(value and BYTE_MASK)

        return this
    }

    fun bool(value: Boolean): BorshWriter = u8(if (value) 1 else 0)

    fun u16(value: Int): BorshWriter = littleEndian(Short.SIZE_BYTES) { putShort(value.toShort()) }

    fun u32(value: Int): BorshWriter = littleEndian(Int.SIZE_BYTES) { putInt(value) }

    fun u64(value: Long): BorshWriter = littleEndian(Long.SIZE_BYTES) { putLong(value) }

    /** Signed 64-bit integer. Identical on the wire to [u64] — two's complement, little-endian. */
    fun i64(value: Long): BorshWriter = u64(value)

    /** 32 raw bytes, not base58. */
    fun pubkey(value: PublicKey): BorshWriter {
        buffer.write(value.toByteArray())

        return this
    }

    /** 4-byte little-endian length, then UTF-8 bytes without a terminator. */
    fun string(value: String): BorshWriter {
        val bytes = value.toByteArray(Charsets.UTF_8)
        u32(bytes.size)
        buffer.write(bytes)

        return this
    }

    /** `0x00` for absent, otherwise `0x01` followed by the value. */
    fun <T : Any> option(value: T?, write: BorshWriter.(T) -> Unit): BorshWriter {
        if (value == null) {
            u8(0)
        } else {
            u8(1)
            write(value)
        }

        return this
    }

    fun toByteArray(): ByteArray = buffer.toByteArray()

    private fun littleEndian(size: Int, write: ByteBuffer.() -> Unit): BorshWriter {
        val bytes = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
        bytes.write()
        buffer.write(bytes.array())

        return this
    }

    private companion object {
        const val BYTE_MASK = 0xFF
    }
}