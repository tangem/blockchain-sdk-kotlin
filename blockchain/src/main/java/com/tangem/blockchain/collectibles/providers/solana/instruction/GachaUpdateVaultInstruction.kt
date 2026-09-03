package com.tangem.blockchain.collectibles.providers.solana.instruction

import com.tangem.blockchain.blockchains.solana.borsh.BorshWriter
import com.tangem.blockchain.collectibles.providers.solana.GachaVaultProgram
import org.p2p.solanaj.core.AccountMeta
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.core.TransactionInstruction
import org.p2p.solanaj.programs.Program

/**
 * `update_vault(hot_delegate: Option<Pubkey>, per_spin_cap: Option<u64>, daily_cap: Option<u64>)` — rotates the hot
 * key and the spending caps of an existing vault.
 */
internal object GachaUpdateVaultInstruction {

    @Suppress("MagicNumber")
    private val DISCRIMINATOR = GachaVaultProgram.discriminator(67, 229, 185, 188, 226, 11, 210, 60)

    /**
     * Accounts are listed strictly in IDL order and with the IDL writable/signer flags — only the vault being written
     * and the cold key authorising the write. There is no payer slot: the instruction allocates nothing, so the
     * transaction's own fee payer covers it.
     *
     * Every argument is optional and a null one leaves the field as it is. All three being null is a valid
     * instruction on chain — it just costs a card tap and a fee to change nothing, which is why the caller rejects it.
     *
     * The caps are validated against the merged state, so a single cap can be refused because of the other one's
     * stored value (`6004 InvalidCaps`); merging is the caller's job, where the error can be raised before signing.
     * `per_spin_cap` cannot be set to zero, so this is not a way to freeze a vault — rotating [hotDelegate] to a key
     * the phone does not hold is.
     */
    fun create(
        vault: PublicKey,
        coldOwner: PublicKey,
        hotDelegate: PublicKey?,
        perSpinCap: Long?,
        dailyCap: Long?,
    ): TransactionInstruction {
        val keys = listOf(
            AccountMeta(vault, /* isSigner = */ false, /* isWritable = */ true),
            AccountMeta(coldOwner, /* isSigner = */ true, /* isWritable = */ false),
        )
        val transactionData = encodeUpdateVaultInstructionData(hotDelegate, perSpinCap, dailyCap)

        return Program.createTransactionInstruction(GachaVaultProgram.programId, keys, transactionData)
    }

    /**
     * 8-byte Anchor discriminator followed by the borsh-encoded arguments, in IDL order.
     *
     * Each `Option` is one tag byte, and only a `Some` is followed by a value — an absent argument is not a zeroed
     * value, and dropping the tag would shift everything after it.
     */
    private fun encodeUpdateVaultInstructionData(
        hotDelegate: PublicKey?,
        perSpinCap: Long?,
        dailyCap: Long?,
    ): ByteArray = DISCRIMINATOR + BorshWriter()
        .option(hotDelegate) { pubkey(it) }
        .option(perSpinCap) { u64(it) }
        .option(dailyCap) { u64(it) }
        .toByteArray()
}