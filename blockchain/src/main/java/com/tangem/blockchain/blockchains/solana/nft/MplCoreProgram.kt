package com.tangem.blockchain.blockchains.solana.nft

import org.p2p.solanaj.core.AccountMeta
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.core.TransactionInstruction
import org.p2p.solanaj.programs.Program

/**
 * Metaplex Core program. A Core asset is a single account of this program — no mint, token account or metadata — and
 * a transfer only rewrites its `owner`.
 */
internal object MplCoreProgram {

    val programId = PublicKey("CoREENxT6tW1HoK8ypY1SxRMZTcVPm7R94rH4PZNhX7d")

    /** `TransferV1` (discriminator 14) with `compression_proof: None`. */
    private val transferV1Data = byteArrayOf(0x0E, 0x00)

    /**
     * All seven accounts are always passed; an absent optional one is replaced by the program id, as the generated
     * clients do. `authority` is absent because the [payer] is the owner, and `system_program` is only needed by
     * compressed assets, whose transfer the program does not support.
     */
    fun transferV1(
        asset: PublicKey,
        collection: PublicKey?,
        payer: PublicKey,
        newOwner: PublicKey,
    ): TransactionInstruction {
        val keys = listOf(
            AccountMeta(asset, /* isSigner = */ false, /* isWritable = */ true),
            AccountMeta(collection ?: programId, /* isSigner = */ false, /* isWritable = */ false),
            AccountMeta(payer, /* isSigner = */ true, /* isWritable = */ true),
            AccountMeta(programId, /* isSigner = */ false, /* isWritable = */ false),
            AccountMeta(newOwner, /* isSigner = */ false, /* isWritable = */ false),
            AccountMeta(programId, /* isSigner = */ false, /* isWritable = */ false),
            AccountMeta(programId, /* isSigner = */ false, /* isWritable = */ false),
        )

        return Program.createTransactionInstruction(programId, keys, transferV1Data)
    }
}