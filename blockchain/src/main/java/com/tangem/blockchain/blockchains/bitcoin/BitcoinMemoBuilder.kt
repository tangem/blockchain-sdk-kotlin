package com.tangem.blockchain.blockchains.bitcoin

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import org.bitcoinj.core.Coin
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionOutput
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
    private const val OP_PUSHDATA1 = 0x4c

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
        // Convert hex string to bytes
        val memoBytes = try {
            hexToBytes(memoHex)
        } catch (e: Exception) {
            return Result.Failure(
                BlockchainSdkError.CustomError("Invalid memo hex string: ${e.message}"),
            )
        }

        // Validate memo size
        if (memoBytes.size > MAX_MEMO_SIZE_BYTES) {
            return Result.Failure(
                BlockchainSdkError.CustomError(
                    "Memo exceeds maximum size of $MAX_MEMO_SIZE_BYTES bytes (got ${memoBytes.size} bytes)",
                ),
            )
        }

        // Build OP_RETURN script
        val script = buildOpReturnScript(memoBytes)

        // Add output with zero value
        transaction.addOutput(Coin.ZERO, script)

        return Result.Success(Unit)
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
        val cleanHex = hex.trim().replace("0x", "", ignoreCase = true)

        require(cleanHex.length % 2 == 0) {
            "Hex string must have even length"
        }

        return cleanHex.chunked(2)
            .map { it.toInt(16).toByte() }
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
        val valueSize = 8
        val scriptLengthVarintSize = 1 // For typical sizes < 253 bytes

        // Script: OP_RETURN (1 byte) + push opcode (1-2 bytes) + data
        val pushOpcodeSize = if (memoSizeBytes <= DIRECT_PUSH_MAX_SIZE) 1 else 2
        val scriptSize = 1 + pushOpcodeSize + memoSizeBytes

        return valueSize + scriptLengthVarintSize + scriptSize
    }
}