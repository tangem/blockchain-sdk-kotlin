package com.tangem.blockchain.blockchains.solana.solanaj.program

import org.p2p.solanaj.core.AccountMeta
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.core.TransactionInstruction
import org.p2p.solanaj.programs.Program

internal object SolanaAssociatedTokenProgram {

    /**
     * `0x01`, the idempotent variant: succeeds and does nothing when the account already exists.
     *
     * [org.p2p.solanaj.programs.AssociatedTokenProgram.createAssociatedTokenAccountInstruction] sends an empty
     * payload, which is `0x00` — it creates the same account but fails with `AccountAlreadyInitialized` if it is
     * already there.
     */
    private const val CREATE_IDEMPOTENT: Byte = 0x01

    /**
     * Creates an associated token account, or does nothing if it already exists.
     *
     * Accounts are listed strictly in the order the program expects them. [owner] does not sign: creation is
     * authorised by [payer] alone, which is what lets this be bundled into somebody else's transaction without
     * adding a signature. That also makes an off-curve [owner] — a PDA — perfectly legal here.
     */
    fun createIdempotentInstruction(
        payer: PublicKey,
        associatedAccount: PublicKey,
        owner: PublicKey,
        mint: PublicKey,
        tokenProgramId: SolanaTokenProgram.ID,
    ): TransactionInstruction {
        val keys = listOf(
            AccountMeta(payer, /* isSigner = */ true, /* isWritable = */ true),
            AccountMeta(associatedAccount, /* isSigner = */ false, /* isWritable = */ true),
            AccountMeta(owner, /* isSigner = */ false, /* isWritable = */ false),
            AccountMeta(mint, /* isSigner = */ false, /* isWritable = */ false),
            AccountMeta(Program.Id.system, /* isSigner = */ false, /* isWritable = */ false),
            AccountMeta(tokenProgramId.value, /* isSigner = */ false, /* isWritable = */ false),
        )

        return Program.createTransactionInstruction(
            Program.Id.splAssociatedTokenAccount,
            keys,
            encodeCreateIdempotentInstructionData(),
        )
    }

    private fun encodeCreateIdempotentInstructionData(): ByteArray = byteArrayOf(CREATE_IDEMPOTENT)
}