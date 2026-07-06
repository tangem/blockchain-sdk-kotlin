package com.tangem.blockchain.blockchains.solana

/** Reads primitives and Solana compact-u16 (shortvec) length prefixes, bounds-checked against the buffer. */
internal class ShortVecReader(private val bytes: ByteArray) {

    private var offset = 0

    fun readU8(): Int {
        if (offset >= bytes.size) throw IndexOutOfBoundsException("Unexpected end of message")
        return bytes[offset++].toInt() and SolanaMessageFormat.BYTE_MASK
    }

    /** Returns the next byte without advancing the cursor. */
    fun peekU8(): Int {
        if (offset >= bytes.size) throw IndexOutOfBoundsException("Unexpected end of message")
        return bytes[offset].toInt() and SolanaMessageFormat.BYTE_MASK
    }

    /** Reads [count] raw bytes and advances the cursor past them. */
    fun readBytes(count: Int): ByteArray {
        if (count < 0 || offset + count > bytes.size) throw IndexOutOfBoundsException("Unexpected end of message")
        val result = bytes.copyOfRange(offset, offset + count)
        offset += count
        return result
    }

    fun skip(count: Int) {
        if (count < 0 || offset + count > bytes.size) throw IndexOutOfBoundsException("Unexpected end of message")
        offset += count
    }

    @Suppress("UnnecessaryParentheses") // parentheses make the and/shl grouping explicit
    fun readShortVec(): Int {
        var result = 0
        var shift = 0
        while (true) {
            val byte = readU8()
            result = result or ((byte and SHORT_VEC_DATA_MASK) shl shift)
            if (byte and SolanaMessageFormat.HIGH_BIT == 0) break
            shift += SHORT_VEC_SHIFT_STEP
            require(shift <= MAX_SHORT_VEC_SHIFT) { "Malformed compact-u16" }
        }
        // A 3-byte encoding can hold up to 0x1FFFFF, but a compact-u16 must stay within the u16 range.
        require(result <= SolanaMessageFormat.MAX_COMPACT_U16) { "Malformed compact-u16" }
        return result
    }

    fun isAtEnd(): Boolean = offset == bytes.size

    private companion object {
        const val SHORT_VEC_DATA_MASK = 0x7F
        const val SHORT_VEC_SHIFT_STEP = 7
        const val MAX_SHORT_VEC_SHIFT = 14 // compact-u16 spans at most 3 bytes (shifts 0, 7, 14)
    }
}