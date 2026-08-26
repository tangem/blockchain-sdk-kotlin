package com.tangem.blockchain.blockchains.solana

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result

object SolanaTransactionHelper {

    private const val SIGNATURE_LENGTH = 64

    /**
     * Returns the serialized message of a compiled transaction — the exact bytes that are signed.
     *
     * A serialized Solana transaction is `[compact-u16 signatureCount][N × 64-byte signature slots][message]`;
     * this strips the leading signature array and returns the remaining message untouched.
     */
    fun extractMessage(transaction: ByteArray): ByteArray = parse(transaction).message

    /**
     * Splices [signature] into the signature slot of a compiled [transaction] that belongs to [signerPublicKey],
     * leaving the signature count and every other slot exactly as received — including placeholders and signatures
     * already supplied by the dApp for co-signers.
     *
     * This preserves multi-signer transactions. The signer's slot index is resolved by matching [signerPublicKey]
     * against the transaction's required-signer keys (the first `numRequiredSignatures` static account keys), so
     * the signature lands at the correct position even when the wallet is not the fee-payer at index 0. Only the
     * 64 bytes of that one slot are overwritten; the rest of the wire bytes are copied verbatim.
     *
     * Never throws — every failure is returned as [Result.Failure]: `SignerPublicKeyNotFound` when
     * [signerPublicKey] is not a required signer, `TransactionIsEmpty` when [transaction] is empty, and a
     * `CustomError` when [signature] is not 64 bytes or [transaction] is malformed.
     */
    fun putSignature(transaction: ByteArray, signerPublicKey: ByteArray, signature: ByteArray): Result<ByteArray> {
        if (signature.size != SIGNATURE_LENGTH) {
            val message = "Solana signature must be $SIGNATURE_LENGTH bytes, got ${signature.size}"
            return Result.Failure(BlockchainSdkError.CustomError(message))
        }

        return try {
            val parsed = parse(transaction)
            val signerKeys = readSignerPublicKeys(parsed.message, maxSigners = parsed.signatures.size)
            val index = signerKeys.indexOfFirst { it.contentEquals(signerPublicKey) }
            if (index !in parsed.signatures.indices) {
                Result.Failure(BlockchainSdkError.Solana.SignerPublicKeyNotFound)
            } else {
                val signedTransaction = transaction.copyOf().also { result ->
                    signature.copyInto(result, destinationOffset = parsed.signaturesOffset + index * SIGNATURE_LENGTH)
                }
                Result.Success(signedTransaction)
            }
        } catch (e: BlockchainSdkError) {
            Result.Failure(e)
        } catch (e: Exception) {
            Result.Failure(BlockchainSdkError.CustomError("Failed to place Solana signature: ${e.message}"))
        }
    }

    @Deprecated(
        message = "Rebuilding the transaction from the returned message drops every other signature slot and " +
            "breaks multi-signer transactions. Use extractMessage() to obtain the bytes to sign and " +
            "putSignature() to reassemble.",
        replaceWith = ReplaceWith("extractMessage(transaction)"),
    )
    fun removeSignaturesPlaceholders(transaction: ByteArray): ByteArray = extractMessage(transaction)

    /**
     * Splits a serialized transaction into its signature slots and message, without interpreting the message.
     */
    private fun parse(transaction: ByteArray): ParsedTransaction {
        if (transaction.isEmpty()) throw BlockchainSdkError.Solana.TransactionIsEmpty

        val reader = ShortVecReader(transaction)
        val signatureCount = reader.readShortVec()
        val signaturesOffset = reader.position()
        val signatures = buildList {
            repeat(signatureCount) { add(reader.readBytes(SIGNATURE_LENGTH)) }
        }
        val message = reader.remaining()

        return ParsedTransaction(
            message = message,
            signatures = signatures,
            signaturesOffset = signaturesOffset,
        )
    }

    /**
     * Reads the required-signer public keys from a serialized [message]: the first `numRequiredSignatures` static
     * account keys, in order. Capped by [maxSigners] (the number of signature slots actually present) so a
     * malformed header cannot read past the slots. Returns an empty list for a message that fails to parse, which
     * surfaces as [BlockchainSdkError.Solana.SignerPublicKeyNotFound] in [putSignature].
     */
    private fun readSignerPublicKeys(message: ByteArray, maxSigners: Int): List<ByteArray> = runCatching {
        val reader = ShortVecReader(message)

        // Versioned (v0) messages prefix the header with `0x80 | version`; legacy messages start with the header.
        val isVersioned = reader.peekU8() and SolanaMessageFormat.HIGH_BIT != 0
        if (isVersioned) reader.readU8() // consume the version prefix

        val numRequiredSignatures = reader.readU8()
        reader.readU8() // numReadonlySigned
        reader.readU8() // numReadonlyUnsigned

        val accountCount = reader.readShortVec()
        val signerCount = minOf(numRequiredSignatures, accountCount, maxSigners)
        buildList {
            repeat(signerCount) { add(reader.readBytes(SolanaMessageFormat.PUBLIC_KEY_LENGTH)) }
        }
    }.getOrDefault(emptyList())

    private class ParsedTransaction(
        /** The serialized message — the bytes that are signed. */
        val message: ByteArray,
        /** Signature slots exactly as received (64 bytes each; zero placeholders or real signatures). */
        val signatures: List<ByteArray>,
        /** Byte offset in the original buffer where the signature slots begin (right after the count prefix). */
        val signaturesOffset: Int,
    )

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