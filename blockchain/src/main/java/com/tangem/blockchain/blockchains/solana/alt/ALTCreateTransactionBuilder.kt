package com.tangem.blockchain.blockchains.solana.alt

import foundation.metaplex.solana.programs.SystemProgram
import foundation.metaplex.solana.transactions.*
import foundation.metaplex.solanapublickeys.PublicKey

// ALT Transaction Builder - Solana KMP
internal object ALTCreateTransactionBuilder {

    private val ALT_PROGRAM_ID = PublicKey("AddressLookupTab1e1111111111111111111111111")

    private const val CREATE_LOOKUP_TABLE: Byte = 0
    private const val EXTEND_LOOKUP_TABLE: Byte = 2

    // Create lookup table instruction
    @Suppress("MagicNumber", "UnnecessaryParentheses")
    private suspend fun createLookupTableInstruction(
        authority: PublicKey,
        payer: PublicKey,
        recentSlot: ULong,
    ): Triple<TransactionInstruction, PublicKey, Byte> {
        // Derive the lookup table address (PDA) with bump
        val seeds = listOf(
            authority.toByteArray(),
            recentSlot.toByteArray(), // Convert UInt64 to little-endian bytes
        )

        val (tableAddress, bump) = PublicKey.findProgramAddress(
            seeds = seeds,
            programId = ALT_PROGRAM_ID,
        )

        // Encode instruction data: [discriminator(4)] + [recentSlot(8)] + [bump(1)]
        val data = ByteArray(13)
        // Instruction index = 0 (4 bytes, little endian)
        data[0] = CREATE_LOOKUP_TABLE
        data[1] = 0
        data[2] = 0
        data[3] = 0
        // recentSlot u64 LE (8 bytes)
        for (i in 0..7) {
            data[4 + i] = ((recentSlot shr (i * 8)) and 0xFFu).toByte()
        }
        // bump seed (1 byte)
        data[12] = bump.toByte()

        // Always include all 4 accounts as expected by the program
        val accountMetas = listOf(
            AccountMeta(tableAddress, isSigner = false, isWritable = true),
            AccountMeta(authority, isSigner = true, isWritable = false),
            AccountMeta(payer, isSigner = true, isWritable = true),
            AccountMeta(SystemProgram.PROGRAM_ID, isSigner = false, isWritable = false),
        )

        val instruction = TransactionInstruction(
            programId = ALT_PROGRAM_ID,
            keys = accountMetas,
            data = data,
        )

        return Triple(instruction, tableAddress, bump.toByte())
    }

    // Extend lookup table instruction
    @Suppress("MagicNumber", "UnnecessaryParentheses")
    private fun extendLookupTableInstruction(
        lookupTable: PublicKey,
        authority: PublicKey,
        payer: PublicKey,
        addresses: List<PublicKey>,
    ): TransactionInstruction {
        val data = ByteArray(4 + 8 + addresses.size * 32)
        data[0] = EXTEND_LOOKUP_TABLE // first 4 bytes is little-endian instruction index
        data[1] = 0
        data[2] = 0
        data[3] = 0
        val count = addresses.size
        for (i in 0..7) {
            data[4 + i] = ((count.toULong() shr (i * 8)) and 0xFFu).toByte()
        }
        var offset = 12
        for (address in addresses) {
            address.toByteArray().copyInto(data, offset)
            offset += 32
        }

        // Always include all 4 accounts as expected by the program
        val accountMetas = listOf(
            AccountMeta(lookupTable, isSigner = false, isWritable = true),
            AccountMeta(authority, isSigner = true, isWritable = false),
            AccountMeta(payer, isSigner = true, isWritable = true),
            AccountMeta(SystemProgram.PROGRAM_ID, isSigner = false, isWritable = false),
        )

        return TransactionInstruction(
            programId = ALT_PROGRAM_ID,
            keys = accountMetas,
            data = data,
        )
    }

    // Create transaction for creating a lookup table
    suspend fun createAndExtendLookupTableTransaction(
        authority: PublicKey,
        payer: PublicKey,
        recentSlot: ULong,
        recentBlockhash: String,
        addresses: List<PublicKey>,
    ): Pair<Transaction, PublicKey> {
        val (instruction, tableAddress, _) = createLookupTableInstruction(
            authority = authority,
            payer = payer,
            recentSlot = recentSlot,
        )

        val instructionExtend = extendLookupTableInstruction(
            lookupTable = tableAddress,
            authority = authority,
            payer = payer,
            addresses = addresses,
        )

        val transaction = SolanaTransactionBuilder()
            .addInstruction(instruction)
            .addInstruction(instructionExtend)
            .setRecentBlockHash(recentBlockhash)
            .build()

        (transaction as SolanaTransaction).feePayer = payer

        return Pair(transaction, tableAddress)
    }
}

// Extension function to convert ULong to little-endian byte array
@Suppress("MagicNumber", "UnnecessaryParentheses")
private fun ULong.toByteArray(): ByteArray {
    val buffer = ByteArray(8)
    for (i in 0..7) {
        buffer[i] = ((this shr (i * 8)) and 0xFFu).toByte()
    }
    return buffer
}