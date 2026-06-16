package com.tangem.blockchain.blockchains.solana.alt

import com.tangem.blockchain.blockchains.solana.ShortVecReader
import com.tangem.blockchain.blockchains.solana.SolanaMessageFormat
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
    fun parse(tx: ByteArray): TransactionRawData {
        val reader = ShortVecReader(tx)

        // Versioned messages set the high bit of the prefix (`0x80 | version`); legacy messages start with the header.
        val firstByte = reader.peekU8()
        val isV0 = firstByte and SolanaMessageFormat.HIGH_BIT != 0
        if (isV0) {
            val version = firstByte and SolanaMessageFormat.HIGH_BIT.inv()
            require(version == SolanaMessageFormat.SUPPORTED_VERSION) { "Unsupported message version: $version" }
            reader.readU8() // consume the version prefix byte
        }
        Logger.logTransaction("SolanaTransactionParser: isV0 = $isV0")

        // Message header
        val requiredSignatures = reader.readU8()
        if (requiredSignatures > 1) {
            Logger.logTransaction("Too many required signatures: $requiredSignatures")
            throw IllegalArgumentException(
                "We support only 1 required signature, but found $requiredSignatures",
            )
        }
        val readonlySignedAccounts = reader.readU8()
        val readonlyUnsignedAccounts = reader.readU8()
        val messageHeader = MessageHeader(
            numRequiredSignatures = requiredSignatures.toByte(),
            numReadonlySignedAccounts = readonlySignedAccounts.toByte(),
            numReadonlyUnsignedAccounts = readonlyUnsignedAccounts.toByte(),
        )

        // Static account keys count (compact-u16)
        val accountCount = reader.readShortVec()
        require(accountCount > 0) { "A Solana message must contain at least the fee-payer account" }

        // Account addresses (each 32 bytes). Read incrementally so a malformed count cannot force a large
        // up-front allocation before the reader reaches the end of the buffer.
        val accountAddresses = buildList {
            repeat(accountCount) { add(reader.readBytes(SolanaMessageFormat.PUBLIC_KEY_LENGTH)) }
        }

        val payer = accountAddresses[0]
        val altAddresses = accountAddresses.drop(1)

        val totalAccounts = accountAddresses.size
        val writableNonsignerEnd = totalAccounts - readonlyUnsignedAccounts
        val writableAltCount = writableNonsignerEnd - 1 // minus payer

        val writableAltAddresses = altAddresses.take(writableAltCount)
        val readonlyAltAddresses = altAddresses.drop(writableAltCount)

        val recentBlockhash = reader.readBytes(SolanaMessageFormat.PUBLIC_KEY_LENGTH)

        // parse instructions
        val instructions = parseInstructions(reader)

        // parse alt data
        val altTables = if (isV0) {
            parseAltData(reader)
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

    private fun parseAltData(reader: ShortVecReader): List<CompiledAltTable> {
        val altTables = mutableListOf<CompiledAltTable>()

        val altCount = reader.readShortVec()

        repeat(altCount) {
            val account = reader.readBytes(SolanaMessageFormat.PUBLIC_KEY_LENGTH)

            // Counts are compact-u16; the index values themselves are single bytes. Read incrementally to avoid
            // pre-allocating from an untrusted count.
            val writableIndexesCount = reader.readShortVec()
            val writableIndexes = buildList { repeat(writableIndexesCount) { add(reader.readU8()) } }

            val readonlyIndexesCount = reader.readShortVec()
            val readonlyIndexes = buildList { repeat(readonlyIndexesCount) { add(reader.readU8()) } }

            altTables.add(
                CompiledAltTable(
                    account = account,
                    writableIndexes = writableIndexes,
                    readonlyIndexes = readonlyIndexes,
                ),
            )
        }

        return altTables
    }

    private fun parseInstructions(reader: ShortVecReader): List<CompiledInstruction> {
        val instructions = mutableListOf<CompiledInstruction>()

        // Instruction count (compact-u16)
        val instructionCount = reader.readShortVec()

        repeat(instructionCount) {
            // Program ID index (1 byte)
            val programIdIndex = reader.readU8()

            // Account count (compact-u16); the indices themselves are single bytes
            val accountCount = reader.readShortVec()

            // Account indices (1 byte each). Read incrementally to avoid pre-allocating from an untrusted count.
            val accountIndices = buildList { repeat(accountCount) { add(reader.readU8()) } }

            // Data length (compact-u16 LEB128)
            val dataLength = reader.readShortVec()

            // Data
            val data = reader.readBytes(dataLength)

            instructions.add(
                CompiledInstruction(
                    programIdIndex = programIdIndex,
                    accounts = accountIndices,
                    data = data.encodeBase58(),
                ),
            )
        }

        return instructions
    }
}

fun Byte.readCompactU16(): Int = this.toInt() and SolanaMessageFormat.BYTE_MASK