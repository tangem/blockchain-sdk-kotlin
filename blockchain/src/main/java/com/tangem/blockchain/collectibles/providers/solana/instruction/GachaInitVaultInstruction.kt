package com.tangem.blockchain.collectibles.providers.solana.instruction

import com.tangem.blockchain.collectibles.providers.solana.GachaVaultProgram
import com.tangem.blockchain.blockchains.solana.borsh.BorshWriter
import org.p2p.solanaj.core.AccountMeta
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.core.TransactionInstruction
import org.p2p.solanaj.programs.Program

/**
 * `init_vault(hot_delegate: Pubkey, per_spin_cap: u64, daily_cap: u64)` — creates the user's vault.
 */
internal object GachaInitVaultInstruction {

    @Suppress("MagicNumber")
    private val DISCRIMINATOR = GachaVaultProgram.discriminator(77, 79, 85, 150, 33, 217, 52, 106)

    /**
     * Accounts are listed strictly in IDL order and with the IDL writable/signer flags. Wrong order is the most
     * common integration failure and surfaces as an error about a completely different account.
     *
     * [hotDelegate] is a plain argument: the program stores it as the key allowed to pay for spins. Where that key
     * comes from is not this module's concern. The caps must satisfy `0 < perSpinCap <= dailyCap`, otherwise the
     * program fails with `6004 InvalidCaps`; the check lives in the caller, where the error can be reported before
     * signing.
     */
    @Suppress("LongParameterList")
    fun create(
        vault: PublicKey,
        coldOwner: PublicKey,
        payer: PublicKey,
        hotDelegate: PublicKey,
        perSpinCap: Long,
        dailyCap: Long,
    ): TransactionInstruction {
        val keys = listOf(
            AccountMeta(vault, /* isSigner = */ false, /* isWritable = */ true),
            AccountMeta(coldOwner, /* isSigner = */ true, /* isWritable = */ false),
            AccountMeta(payer, /* isSigner = */ true, /* isWritable = */ true),
            AccountMeta(GachaVaultProgram.systemProgramId, /* isSigner = */ false, /* isWritable = */ false),
        )
        val transactionData = encodeInitVaultInstructionData(hotDelegate, perSpinCap, dailyCap)

        return Program.createTransactionInstruction(GachaVaultProgram.programId, keys, transactionData)
    }

    /** 8-byte Anchor discriminator followed by the borsh-encoded arguments, in IDL order. */
    private fun encodeInitVaultInstructionData(hotDelegate: PublicKey, perSpinCap: Long, dailyCap: Long): ByteArray =
        DISCRIMINATOR + BorshWriter()
            .pubkey(hotDelegate)
            .u64(perSpinCap)
            .u64(dailyCap)
            .toByteArray()
}