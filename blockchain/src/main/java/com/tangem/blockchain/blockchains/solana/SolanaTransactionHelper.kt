package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.common.BlockchainSdkError

object SolanaTransactionHelper {

    private const val SIGNATURE_LENGTH = 64

    /**
     * Removes signatures placeholders from transaction data
     * @param transaction: transaction data with placeholders
     * @returns Transaction data without placeholders
     */
    fun removeSignaturesPlaceholders(transaction: ByteArray): ByteArray {
        val firstByte = transaction.firstOrNull() ?: throw BlockchainSdkError.Solana.TransactionIsEmpty

        val signaturesPlaceholderLength = 1 + firstByte.toInt() * SIGNATURE_LENGTH

        return transaction.drop(signaturesPlaceholderLength).toByteArray()
    }

    /**
     * Checks whether [data] is a serialized Solana transaction message (legacy or v0).
     *
     * Solana signs the serialized message of a transaction, so signing such bytes through an off-chain
     * `signMessage` request would produce a signature that is also a valid transaction signature — a malicious
     * dApp could then broadcast it and move the user's funds. Callers must reject `signMessage` payloads for which
     * this returns `true` instead of blind-signing them.
     *
     * The check is strict: the bytes must parse as a well-formed message AND be consumed in full (no leftover
     * bytes). Human-readable sign-in messages — the legitimate use of `signMessage` — do not satisfy the structural
     * constraints and are reported as `false`.
     */
    fun isTransactionMessage(data: ByteArray): Boolean {
        if (data.isEmpty()) return false

        return runCatching { parseAsTransactionMessage(data) }.getOrDefault(false)
    }

    /**
     * Strictly validates that [data] is a serialized Solana message. Returns `true` only when the whole buffer is
     * consumed by a well-formed legacy or v0 message — leftover bytes or malformed structure yield `false`.
     */
    @Suppress("ReturnCount")
    private fun parseAsTransactionMessage(data: ByteArray): Boolean {
        val reader = ShortVecReader(data)

        // Versioned (v0) messages prefix the header with `0x80 | version`; legacy messages start with the header.
        val firstByte = data[0].toInt() and SolanaMessageFormat.BYTE_MASK
        val isVersioned = firstByte and SolanaMessageFormat.HIGH_BIT != 0
        if (isVersioned) {
            // only v0 exists today
            if (firstByte and SolanaMessageFormat.HIGH_BIT.inv() != SolanaMessageFormat.SUPPORTED_VERSION) return false
            reader.readU8() // consume version prefix
        }

        // Message header: numRequiredSignatures, numReadonlySigned, numReadonlyUnsigned.
        val numRequiredSignatures = reader.readU8()
        val numReadonlySigned = reader.readU8()
        val numReadonlyUnsigned = reader.readU8()
        if (numRequiredSignatures == 0) return false // a transaction always has at least the fee payer

        // Static account keys.
        val accountCount = reader.readShortVec()
        if (accountCount == 0) return false
        if (numRequiredSignatures > accountCount) return false
        // Readonly signers are a subset of the signers, readonly non-signers a subset of the non-signers.
        if (numReadonlySigned > numRequiredSignatures) return false
        if (numReadonlyUnsigned > accountCount - numRequiredSignatures) return false
        reader.skip(accountCount * SolanaMessageFormat.PUBLIC_KEY_LENGTH)

        // Recent blockhash.
        reader.skip(SolanaMessageFormat.PUBLIC_KEY_LENGTH)

        // Compiled instructions.
        val instructionCount = reader.readShortVec()
        repeat(instructionCount) {
            val programIdIndex = reader.readU8()
            // In legacy messages the program id must reference a static account key; v0 may resolve it via a table.
            if (!isVersioned && programIdIndex >= accountCount) return false

            // Account indexes (one byte each). In legacy messages each must reference a static account key.
            val accountIndexCount = reader.readShortVec()
            repeat(accountIndexCount) {
                val accountIndex = reader.readU8()
                if (!isVersioned && accountIndex >= accountCount) return false
            }

            reader.skip(reader.readShortVec()) // instruction data
        }

        // Address table lookups (versioned messages only).
        if (isVersioned) {
            val lookupCount = reader.readShortVec()
            repeat(lookupCount) {
                reader.skip(SolanaMessageFormat.PUBLIC_KEY_LENGTH) // lookup table account key
                reader.skip(reader.readShortVec()) // writable indexes
                reader.skip(reader.readShortVec()) // readonly indexes
            }
        }

        // A genuine message consumes the whole buffer with nothing left over.
        return reader.isAtEnd()
    }
}