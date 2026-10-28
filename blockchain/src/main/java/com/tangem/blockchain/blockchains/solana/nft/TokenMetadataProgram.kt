package com.tangem.blockchain.blockchains.solana.nft

import org.p2p.solanaj.core.AccountMeta
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.core.TransactionInstruction
import org.p2p.solanaj.programs.Program
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Metaplex Token Metadata program, as far as a programmable NFT (pNFT) transfer needs it.
 *
 * A pNFT's token account is frozen, and only this program can thaw it for the duration of a transfer, so a plain SPL
 * `transferChecked` of a pNFT always fails.
 */
internal object TokenMetadataProgram {

    val programId = PublicKey("metaqbxxUerdq28cj1RbAWkYQm3ybzjb6a8bt518x1s")

    private val authorizationRulesProgramId = PublicKey("auth9SigNpDKz4sJJ1DfCTuZrZNSAgh9sFD3rboVmgg")
    private val sysvarInstructionsId = PublicKey("Sysvar1nstructions1111111111111111111111111")

    private val metadataSeed = "metadata".toByteArray()
    private val editionSeed = "edition".toByteArray()
    private val tokenRecordSeed = "token_record".toByteArray()

    private const val TRANSFER_DISCRIMINATOR: Byte = 49
    private const val TRANSFER_ARGS_V1: Byte = 0
    private const val NONE: Byte = 0
    private const val AMOUNT: Long = 1
    private const val TRANSFER_DATA_SIZE = 3 * Byte.SIZE_BYTES + Long.SIZE_BYTES

    fun metadata(mint: PublicKey): PublicKey = findAddress(metadataSeed, programId.toByteArray(), mint.toByteArray())

    fun masterEdition(mint: PublicKey): PublicKey =
        findAddress(metadataSeed, programId.toByteArray(), mint.toByteArray(), editionSeed)

    fun tokenRecord(mint: PublicKey, token: PublicKey): PublicKey =
        findAddress(metadataSeed, programId.toByteArray(), mint.toByteArray(), tokenRecordSeed, token.toByteArray())

    /**
     * `Transfer` with `TransferArgs::V1 { amount: 1, authorization_data: None }`. The owner signs both as authority
     * and as payer of the destination token record.
     *
     * Without a [ruleSet] both authorization accounts are replaced by the program id, the way Metaplex clients encode
     * an absent optional account.
     */
    @Suppress("LongParameterList")
    fun transfer(
        mint: PublicKey,
        owner: PublicKey,
        ownerToken: PublicKey,
        destinationOwner: PublicKey,
        destinationToken: PublicKey,
        ruleSet: PublicKey?,
        tokenProgram: PublicKey,
    ): TransactionInstruction {
        val keys = listOf(
            AccountMeta(ownerToken, /* isSigner = */ false, /* isWritable = */ true),
            AccountMeta(owner, /* isSigner = */ false, /* isWritable = */ false),
            AccountMeta(destinationToken, /* isSigner = */ false, /* isWritable = */ true),
            AccountMeta(destinationOwner, /* isSigner = */ false, /* isWritable = */ false),
            AccountMeta(mint, /* isSigner = */ false, /* isWritable = */ false),
            AccountMeta(metadata(mint), /* isSigner = */ false, /* isWritable = */ true),
            AccountMeta(masterEdition(mint), /* isSigner = */ false, /* isWritable = */ false),
            AccountMeta(tokenRecord(mint, ownerToken), /* isSigner = */ false, /* isWritable = */ true),
            AccountMeta(tokenRecord(mint, destinationToken), /* isSigner = */ false, /* isWritable = */ true),
            AccountMeta(owner, /* isSigner = */ true, /* isWritable = */ false),
            AccountMeta(owner, /* isSigner = */ true, /* isWritable = */ true),
            AccountMeta(Program.Id.system, /* isSigner = */ false, /* isWritable = */ false),
            AccountMeta(sysvarInstructionsId, /* isSigner = */ false, /* isWritable = */ false),
            AccountMeta(tokenProgram, /* isSigner = */ false, /* isWritable = */ false),
            AccountMeta(Program.Id.splAssociatedTokenAccount, /* isSigner = */ false, /* isWritable = */ false),
            AccountMeta(
                if (ruleSet != null) authorizationRulesProgramId else programId,
                /* isSigner = */ false,
                /* isWritable = */ false,
            ),
            AccountMeta(ruleSet ?: programId, /* isSigner = */ false, /* isWritable = */ false),
        )

        return Program.createTransactionInstruction(programId, keys, transferData())
    }

    private fun transferData(): ByteArray = ByteBuffer.allocate(TRANSFER_DATA_SIZE)
        .order(ByteOrder.LITTLE_ENDIAN)
        .put(TRANSFER_DISCRIMINATOR)
        .put(TRANSFER_ARGS_V1)
        .putLong(AMOUNT)
        .put(NONE)
        .array()

    private fun findAddress(vararg seeds: ByteArray): PublicKey =
        PublicKey.findProgramAddress(seeds.toList(), programId).address
}