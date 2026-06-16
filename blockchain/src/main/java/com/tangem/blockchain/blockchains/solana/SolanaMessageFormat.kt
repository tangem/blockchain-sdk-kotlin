package com.tangem.blockchain.blockchains.solana

/**
 * Wire-format constants shared across Solana transaction-message parsing — [ShortVecReader],
 * [SolanaTransactionHelper] and the `alt` transaction parser. Kept in one place so the magic numbers of the
 * serialized message format are defined once instead of being duplicated per file.
 */
internal object SolanaMessageFormat {

    /** Length of a Solana account public key — and of the recent blockhash — in bytes. */
    const val PUBLIC_KEY_LENGTH = 32

    /** Mask to read a single byte as an unsigned 0..255 value. */
    const val BYTE_MASK = 0xFF

    /**
     * The high bit (`0x80`). In compact-u16 it flags a continuation byte, so any value below it fits in a single
     * byte; versioned (v0) messages reuse the very same bit as the prefix that distinguishes them from legacy ones.
     */
    const val HIGH_BIT = 0x80

    /** The only versioned message format that exists today (v0). The version lives in the low bits of the prefix. */
    const val SUPPORTED_VERSION = 0

    /** Largest value a compact-u16 (shortvec) length prefix may encode. */
    const val MAX_COMPACT_U16 = 0xFFFF
}