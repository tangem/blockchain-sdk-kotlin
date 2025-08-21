package com.tangem.blockchain.blockchains.solana.alt

import com.tangem.blockchain.common.logging.Logger
import com.tangem.blockchain.extensions.encodeBase58
import com.tangem.crypto.decodeBase58
import foundation.metaplex.solana.transactions.AccountMeta
import foundation.metaplex.solana.transactions.CompiledInstruction
import foundation.metaplex.solana.transactions.MessageHeader
import foundation.metaplex.solana.transactions.TransactionInstruction
import foundation.metaplex.solanapublickeys.PublicKey

internal class SolanaTransactionParser {

    fun convertCompiledToTransactionInstructions(
        compiledInstructions: List<CompiledInstruction>,
        allAccountAddresses: List<ByteArray>,
        requiredSignatures: Int,
        readonlySignedAccounts: Int,
        readonlyUnsignedAccounts: Int,
    ): List<TransactionInstruction> {
        return compiledInstructions.map { instruction ->
            TransactionInstruction(
                programId = PublicKey(allAccountAddresses[instruction.programIdIndex]),
                keys = instruction.accounts.map { idx ->
                    val isSigner = idx < requiredSignatures
                    val isWritable = when {
                        idx < requiredSignatures - readonlySignedAccounts -> true
                        idx >= requiredSignatures && idx < allAccountAddresses.size - readonlyUnsignedAccounts -> true
                        else -> false
                    }
                    AccountMeta(
                        publicKey = PublicKey(allAccountAddresses[idx]),
                        isSigner = isSigner,
                        isWritable = isWritable,
                    )
                },
                data = instruction.data.decodeBase58(),
            )
        }
    }

    /**
     * Parses solana transaction and extracts required addresses and data.
     */
    @Suppress("MagicNumber")
    fun parse(tx: ByteArray): TransactionRawData {
        var offset = 0

        val isV0 = if (tx[0].readCompactU16() == 0x80) {
            offset += 1 // Skip the first byte for v0 transactions
            true
        } else {
            false
        }
        Logger.logTransaction("SolanaTransactionParser: isV0 = $isV0")

        // Message header
        val requiredSignatures = tx.readCompactU16(offset)
        if (requiredSignatures > 1) {
            Logger.logTransaction("Too many required signatures: $requiredSignatures")
            throw IllegalArgumentException(
                "We support only 1 required signature, but found $requiredSignatures",
            )
        }
        val readonlySignedAccounts = tx.readCompactU16(offset + 1)
        val readonlyUnsignedAccounts = tx.readCompactU16(offset + 2)
        val messageHeader = MessageHeader(
            numRequiredSignatures = requiredSignatures.toByte(),
            numReadonlySignedAccounts = readonlySignedAccounts.toByte(),
            numReadonlyUnsignedAccounts = readonlyUnsignedAccounts.toByte(),
        )
        offset += 3

        // Account addresses count (compact-u16, single byte for legacy tx)
        val accountCount = tx.readCompactU16(offset)
        offset += 1

        if (accountCount >= 0x80) {
            throw IllegalArgumentException(
                "Account count uses more than 1 byte, not supported for legacy tx\n" +
                    "Probably it is already a v0 transaction",
            )
        }

        // Account addresses (each 32 bytes)
        val accountAddresses = mutableListOf<ByteArray>()
        repeat(accountCount) {
            accountAddresses.add(tx.copyOfRange(offset, offset + 32))
            offset += 32
        }

        val payer = accountAddresses[0]
        val altAddresses = accountAddresses.drop(1)

        val totalAccounts = accountAddresses.size
        val writableNonsignerEnd = totalAccounts - readonlyUnsignedAccounts
        val writableAltCount = writableNonsignerEnd - 1 // minus payer

        val writableAltAddresses = altAddresses.take(writableAltCount)
        val readonlyAltAddresses = altAddresses.drop(writableAltCount)

        val recentBlockhash = tx.copyOfRange(offset, offset + 32)
        offset += 32

        // parse instructions
        val (instructions, newOffset) = parseInstructions(tx = tx, offset = offset)

        offset = newOffset

        // parse alt data
        val altTables = if (isV0) {
            parseAltData(tx, offset).first
        } else {
            null
        }

        return TransactionRawData(
            payer = payer,
            staticAccountAddresses = accountAddresses,
            writableAltAddresses = writableAltAddresses,
            readonlyAltAddresses = readonlyAltAddresses,
            recentBlockhash = recentBlockhash,
            compiledInstructions = instructions,
            messageHeader = messageHeader,
            compiledAltTable = altTables,
        )
    }

    @Suppress("MagicNumber")
    private fun parseAltData(tx: ByteArray, offset: Int): Pair<List<CompiledAltTable>, Int> {
        var currentOffset = offset
        val altTables = mutableListOf<CompiledAltTable>()

        val altCount = tx.readCompactU16(currentOffset)
        currentOffset += 1

        repeat(altCount) {
            val account = tx.copyOfRange(currentOffset, currentOffset + 32)
            currentOffset += 32

            val writableIndexesCount = tx.readCompactU16(currentOffset)
            currentOffset += 1
            val writableIndexes = mutableListOf<Int>()
            repeat(writableIndexesCount) {
                writableIndexes.add(tx.readCompactU16(currentOffset))
                currentOffset += 1
            }

            val readonlyIndexesCount = tx.readCompactU16(currentOffset)
            currentOffset += 1
            val readonlyIndexes = mutableListOf<Int>()
            repeat(readonlyIndexesCount) {
                readonlyIndexes.add(tx.readCompactU16(currentOffset))
                currentOffset += 1
            }

            altTables.add(
                CompiledAltTable(
                    account = account,
                    writableIndexes = writableIndexes,
                    readonlyIndexes = readonlyIndexes,
                ),
            )
        }

        return altTables to currentOffset
    }

    private fun parseInstructions(tx: ByteArray, offset: Int): Pair<List<CompiledInstruction>, Int> {
        var currentOffset = offset
        val instructions = mutableListOf<CompiledInstruction>()

        // Instruction count
        val instructionCount = tx.readCompactU16(currentOffset)
        currentOffset += 1

        repeat(instructionCount) {
            // Program ID index (1 byte)
            val programIdIndex = tx.readCompactU16(currentOffset)
            currentOffset += 1

            // Account count (1 byte)
            val accountCount = tx.readCompactU16(currentOffset)
            currentOffset += 1

            // Account indices (accountCount * 2 bytes)
            val accountIndices = tx.copyOfRange(currentOffset, currentOffset + accountCount).map { it.readCompactU16() }
            currentOffset += accountCount

            // Data length (compact-u16 LEB128)
            val (dataLengthWithOffset, updatedOffset) = tx.readCompactU16Leb128(currentOffset)
            val dataLength = dataLengthWithOffset
            currentOffset = updatedOffset

            // Data
            val data = tx.copyOfRange(currentOffset, currentOffset + dataLength)
            currentOffset += dataLength

            instructions.add(

                CompiledInstruction(
                    programIdIndex = programIdIndex,
                    accounts = accountIndices,
                    data = data.encodeBase58(),
                ),
            )
        }

        return instructions to currentOffset
    }
}

/**
 * Reads a single-byte compact-u16 value from the given offset in the ByteArray.
 * Returns the value as an Int (0..255).
 */
@Suppress("MagicNumber")
fun ByteArray.readCompactU16(offset: Int): Int = this[offset].toInt() and 0xFF

@Suppress("MagicNumber")
fun Byte.readCompactU16(): Int = this.toInt() and 0xFF

/**
 * Reads a compact-u16 LEB128 value from the given offset in the ByteArray.
 * Returns a Pair containing the value as an Int and the new offset after reading.
 */
@Suppress("MagicNumber", "UnnecessaryParentheses")
fun ByteArray.readCompactU16Leb128(offset: Int): Pair<Int, Int> {
    var result = 0
    var shift = 0
    var currentOffset = offset

    while (true) {
        val byte = this[currentOffset].toInt() and 0xFF
        result = result or ((byte and 0x7F) shl shift)
        currentOffset++
        if ((byte and 0x80) == 0) break
        shift += 7
    }
    return result to currentOffset
}