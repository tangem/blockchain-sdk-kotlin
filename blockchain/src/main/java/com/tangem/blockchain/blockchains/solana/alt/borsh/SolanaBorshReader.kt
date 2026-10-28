package com.tangem.blockchain.blockchains.solana.alt.borsh

import org.p2p.solanaj.core.PublicKey
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class SolanaBorshException(message: String) : IllegalArgumentException(message)

/** Sequential reader of Borsh-encoded Solana account data; every overrun throws [SolanaBorshException]. */
internal class SolanaBorshReader(private val data: ByteArray, private var position: Int = 0) {

    val isAtEnd: Boolean get() = position >= data.size

    fun bytes(count: Int): ByteArray {
        if (count < 0 || position + count > data.size) throw SolanaBorshException("Account data is truncated")
        return data.copyOfRange(position, position + count).also { position += count }
    }

    fun skip(count: Int) {
        bytes(count)
    }

    fun u8(): Int = bytes(1)[0].toInt() and BYTE_MASK

    fun bool(): Boolean = when (u8()) {
        0 -> false
        1 -> true
        else -> throw SolanaBorshException("Invalid bool")
    }

    fun u32(): Long = ByteBuffer.wrap(bytes(Int.SIZE_BYTES)).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and U32_MASK

    fun u64(): Long = ByteBuffer.wrap(bytes(Long.SIZE_BYTES)).order(ByteOrder.LITTLE_ENDIAN).long

    fun offset(): Int {
        val value = u64()
        if (value < 0 || value > data.size) throw SolanaBorshException("Offset $value is out of the account")
        return value.toInt()
    }

    fun publicKey(): PublicKey = PublicKey(bytes(PUBLIC_KEY_SIZE))

    fun skipString() {
        skip(length())
    }

    fun <T> option(read: (SolanaBorshReader) -> T): T? = if (bool()) read(this) else null

    fun vector(read: (SolanaBorshReader) -> Unit) {
        repeat(length()) { read(this) }
    }

    fun at(offset: Int): SolanaBorshReader {
        if (offset < 0 || offset > data.size) throw SolanaBorshException("Offset $offset is out of the account")
        return SolanaBorshReader(data, offset)
    }

    private fun length(): Int {
        val length = u32()
        if (length > data.size - position) throw SolanaBorshException("Length $length is out of the account")
        return length.toInt()
    }

    companion object {
        const val PUBLIC_KEY_SIZE = 32
        private const val BYTE_MASK = 0xFF
        private const val U32_MASK = 0xFFFFFFFFL
    }
}