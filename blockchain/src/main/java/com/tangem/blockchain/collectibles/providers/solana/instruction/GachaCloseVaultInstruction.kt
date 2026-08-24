package com.tangem.blockchain.collectibles.providers.solana.instruction

import com.tangem.blockchain.collectibles.providers.solana.GachaVaultProgram
import org.p2p.solanaj.core.AccountMeta
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.core.TransactionInstruction
import org.p2p.solanaj.programs.Program

/**
 * `close_vault()` — closes the vault and returns its rent to `cold_owner`.
 */
internal object GachaCloseVaultInstruction {

    @Suppress("MagicNumber")
    private val DISCRIMINATOR = GachaVaultProgram.discriminator(141, 103, 17, 126, 72, 75, 29, 29)

    /**
     * Fails on-chain with `6014 VaultNotClean` while a buyback delegate is live. The vault must also be closed after

     * the same cold key. Enforcing that order is the caller's responsibility.
     */
    fun create(vault: PublicKey, coldOwner: PublicKey): TransactionInstruction {
        val keys = listOf(
            AccountMeta(vault, /* isSigner = */ false, /* isWritable = */ true),
            AccountMeta(coldOwner, /* isSigner = */ true, /* isWritable = */ true),
        )

        return Program.createTransactionInstruction(
            GachaVaultProgram.programId,
            keys,
            encodeCloseVaultInstructionData(),
        )
    }

    /** No arguments, so the discriminator is the whole payload. Copied so callers cannot mutate the shared array. */
    private fun encodeCloseVaultInstructionData(): ByteArray = DISCRIMINATOR.copyOf()
}