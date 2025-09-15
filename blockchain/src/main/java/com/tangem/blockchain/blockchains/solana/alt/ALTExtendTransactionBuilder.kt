package com.tangem.blockchain.blockchains.solana.alt

import foundation.metaplex.solanapublickeys.PublicKey
import foundation.metaplex.solana.transactions.*
import foundation.metaplex.solana.programs.SystemProgram

// ALT Transaction Builder - Solana KMP
internal object ALTExtendTransactionBuilder {

    private val ALT_PROGRAM_ID = PublicKey("AddressLookupTab1e1111111111111111111111111")

    private const val EXTEND_LOOKUP_TABLE: Byte = 2

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
            data[4 + i] = ((count.toULong() shr (i * 8)) and 0xFFu).toByte() // use parentheses
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

    // Create transaction for extending a lookup table
    suspend fun extendLookupTableTransaction(
        lookupTable: PublicKey,
        authority: PublicKey,
        payer: PublicKey,
        addresses: List<PublicKey>,
        recentBlockhash: String,
    ): Transaction {
        val instruction = extendLookupTableInstruction(
            lookupTable = lookupTable,
            authority = authority,
            payer = payer,
            addresses = addresses,
        )

        val transaction = SolanaTransactionBuilder()
            .addInstruction(instruction)
            .setRecentBlockHash(recentBlockhash)
            // .setSigners(listOf(getDummySigner(authority))) // commented, not needed for this transaction
            // but after that we must specify fee payer explicitly
            .build()

        (transaction as SolanaTransaction).feePayer = payer

        return transaction
    }
}