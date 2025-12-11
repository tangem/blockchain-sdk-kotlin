package com.tangem.blockchain.blockchains.bitcoin

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import org.bitcoinj.core.Coin
import org.bitcoinj.core.Transaction
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.script.ScriptOpCodes

/**
 * Builder for creating OP_RETURN outputs with memo data for Bitcoin transactions.
 *
 * OP_RETURN outputs allow embedding arbitrary data in Bitcoin transactions.
 * The memo data is not stored in the UTXO set and cannot be spent.
 *
 * Specification:
 * - Maximum memo size: 80 bytes (may increase in future)
 * - Output value: 0 satoshis
 * - Script format: OP_RETURN + push opcode + data
 *
 * @see <a href="https://en.bitcoin.it/wiki/OP_RETURN">OP_RETURN Documentation</a>
 */
object BitcoinMemoBuilder {

    private const val MAX_MEMO_SIZE_BYTES = 80
    private const val DIRECT_PUSH_MAX_SIZE = 75
    private const val HEX_RADIX = 16
    private const val OUTPUT_VALUE_SIZE = 8
    private const val OP_RETURN_SIZE = 1

    /**
     * Adds an OP_RETURN output with memo data to a transaction.
     *
     * @param transaction The transaction to add the memo output to
     * @param memoHex Memo data as hex string (without 0x prefix)
     * @return Success if memo was added, Failure if memo exceeds size limit or is invalid
     *
     * @throws IllegalArgumentException if memoHex contains invalid hex characters
     */
    fun addMemoOutput(transaction: Transaction, memoHex: String): Result<Unit> {
        val memoBytes = parseMemoHex(memoHex).getOrElse { return it }
        validateMemoSize(memoBytes).getOrElse { return it }

        val script = buildOpReturnScript(memoBytes)
        transaction.addOutput(Coin.ZERO, script)

        return Result.Success(Unit)
    }

    /**
     * Parses memo hex string to bytes.
     */
    private fun parseMemoHex(memoHex: String): Result<ByteArray> {
        return try {
            Result.Success(hexToBytes(memoHex))
        } catch (e: Exception) {
            Result.Failure(
                BlockchainSdkError.CustomError("Invalid memo hex string: ${e.message}"),
            )
        }
    }

    /**
     * Validates memo size.
     */
    private fun validateMemoSize(memoBytes: ByteArray): Result<Unit> {
        return if (memoBytes.size > MAX_MEMO_SIZE_BYTES) {
            Result.Failure(
                BlockchainSdkError.CustomError(
                    "Memo exceeds maximum size of $MAX_MEMO_SIZE_BYTES bytes (got ${memoBytes.size} bytes)",
                ),
            )
        } else {
            Result.Success(Unit)
        }
    }

    /**
     * Extension to handle Result unwrapping.
     */
    private inline fun <T> Result<T>.getOrElse(onFailure: (Result.Failure) -> Nothing): T {
        return when (this) {
            is Result.Success -> this.data
            is Result.Failure -> onFailure(this)
        }
    }

    /**
     * Creates an OP_RETURN script with the given data.
     *
     * Script structure:
     * - OP_RETURN (0x6a)
     * - Push opcode (length byte or OP_PUSHDATA1 + length)
     * - Data bytes
     *
     * @param data The data to embed in the script
     * @return The constructed script
     */
    private fun buildOpReturnScript(data: ByteArray): org.bitcoinj.script.Script {
        val scriptBuilder = ScriptBuilder()
            .op(ScriptOpCodes.OP_RETURN)

        // Add data with appropriate push opcode
        return scriptBuilder.data(data).build()
    }

    /**
     * Converts hex string to byte array.
     *
     * @param hex Hex string (without 0x prefix)
     * @return Byte array representation
     * @throws IllegalArgumentException if hex string is invalid
     */
    private fun hexToBytes(hex: String): ByteArray {
        val cleanHex = hex.trim().removePrefix("0x").removePrefix("0X")

        require(cleanHex.length % 2 == 0) {
            "Hex string must have even length"
        }

        return cleanHex.chunked(2)
            .map { it.toInt(HEX_RADIX).toByte() }
            .toByteArray()
    }

    /**
     * Estimates the size contribution of an OP_RETURN output.
     *
     * @param memoSizeBytes Size of memo data in bytes
     * @return Estimated size in bytes
     */
    fun estimateOpReturnOutputSize(memoSizeBytes: Int): Int {
        // Output structure: value (8 bytes) + script length varint + script
        val scriptLengthVarintSize = 1 // For typical sizes < 253 bytes

        // Script: OP_RETURN (1 byte) + push opcode (1-2 bytes) + data
        val pushOpcodeSize = if (memoSizeBytes <= DIRECT_PUSH_MAX_SIZE) 1 else 2
        val scriptSize = OP_RETURN_SIZE + pushOpcodeSize + memoSizeBytes

        return OUTPUT_VALUE_SIZE + scriptLengthVarintSize + scriptSize
    }
}