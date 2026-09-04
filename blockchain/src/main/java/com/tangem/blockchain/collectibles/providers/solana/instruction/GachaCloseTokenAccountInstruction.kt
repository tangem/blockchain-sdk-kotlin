package com.tangem.blockchain.collectibles.providers.solana.instruction

import com.tangem.blockchain.blockchains.solana.solanaj.program.SolanaTokenProgram
import com.tangem.blockchain.collectibles.providers.solana.GachaVaultProgram
import org.p2p.solanaj.core.AccountMeta
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.core.TransactionInstruction
import org.p2p.solanaj.programs.Program

/**
 * `close_token_account()` — closes an empty token account owned by the vault and returns its rent to `cold_owner`.
 */
internal object GachaCloseTokenAccountInstruction {

    @Suppress("MagicNumber")
    private val DISCRIMINATOR = GachaVaultProgram.discriminator(132, 172, 24, 60, 100, 156, 135, 97)

    /**
     * Accounts are listed strictly in IDL order and with the IDL writable/signer flags.
     *
     * The vault only authorises the close as the account's owner — it is the PDA that signs the inner CPI, so it is
     * neither a signer nor writable here. `cold_owner` is writable because the rent lands on it.
     *
     * Fails on-chain with `6015 TokenAccountNotEmpty` while the account holds a balance, `6016 DelegateStillSet` while
     * an SPL delegate is live, and `6013 BuybackStillActive` for the account recorded in the vault's buyback slot.
     * The account must exist: a closed one gives `3012 AccountNotInitialized`, which is why the caller decides whether
     * to include this instruction at all.
     *

     * to be closed by its `sweep_prize_ata` cron, which refunds the rent to Collector Crypt. This instruction claims
     * the same lamports for the user, so it belongs on accounts the user paid for — the vault's USDC account.
     */
    fun create(vault: PublicKey, coldOwner: PublicKey, tokenAccount: PublicKey): TransactionInstruction {
        val keys = listOf(
            AccountMeta(vault, /* isSigner = */ false, /* isWritable = */ false),
            AccountMeta(coldOwner, /* isSigner = */ true, /* isWritable = */ true),
            AccountMeta(tokenAccount, /* isSigner = */ false, /* isWritable = */ true),
            AccountMeta(SolanaTokenProgram.ID.TOKEN.value, /* isSigner = */ false, /* isWritable = */ false),
        )

        return Program.createTransactionInstruction(
            GachaVaultProgram.programId,
            keys,
            encodeCloseTokenAccountInstructionData(),
        )
    }

    /** No arguments, so the discriminator is the whole payload. Copied so callers cannot mutate the shared array. */
    private fun encodeCloseTokenAccountInstructionData(): ByteArray = DISCRIMINATOR.copyOf()
}