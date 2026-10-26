package com.tangem.blockchain.collectibles.providers.solana.instruction

import com.tangem.blockchain.blockchains.solana.borsh.BorshWriter
import com.tangem.blockchain.blockchains.solana.solanaj.program.SolanaTokenProgram
import com.tangem.blockchain.collectibles.providers.solana.GachaVaultProgram
import org.p2p.solanaj.core.AccountMeta
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.core.TransactionInstruction
import org.p2p.solanaj.programs.Program

/**
 * `purchase_pack(amount: u64, fee_amount: u64, memo: String)` — pays for one spin from the vault, signed by the
 * session key (`hot_delegate`) alone. [feeAmount] is charged on top of [amount] into the fee treasury.
 *
 * The program does not deduplicate [memo]: two purchases with the same one charge the vault twice while the gacha
 * counts a single spin. Only resending the very same signed transaction is safe.
 */
internal object GachaPurchasePackInstruction {

    @Suppress("MagicNumber")
    private val DISCRIMINATOR = GachaVaultProgram.discriminator(43, 116, 135, 75, 251, 67, 81, 26)

    /**
     * Accounts strictly in IDL order. The treasuries are the ones in the on-chain config, pinned by the program.
     *
     * @param sessionKey user's hot key, the signer
     * @param vaultUsdc  user's USDC account
     * @param gachaUsdc  service provider's USDC account
     * @param feeUsdc    our fee USDC account
     */
    @Suppress("LongParameterList")
    fun create(
        config: PublicKey,
        vault: PublicKey,
        sessionKey: PublicKey,
        usdcMint: PublicKey,
        vaultUsdc: PublicKey,
        gachaUsdc: PublicKey,
        feeUsdc: PublicKey,
        amount: Long,
        feeAmount: Long,
        memo: String,
    ): TransactionInstruction {
        val keys = listOf(
            AccountMeta(config, /* isSigner = */ false, /* isWritable = */ false),
            AccountMeta(vault, /* isSigner = */ false, /* isWritable = */ true),
            AccountMeta(sessionKey, /* isSigner = */ true, /* isWritable = */ false),
            AccountMeta(usdcMint, /* isSigner = */ false, /* isWritable = */ false),
            AccountMeta(vaultUsdc, /* isSigner = */ false, /* isWritable = */ true),
            AccountMeta(gachaUsdc, /* isSigner = */ false, /* isWritable = */ true),
            AccountMeta(feeUsdc, /* isSigner = */ false, /* isWritable = */ true),
            AccountMeta(SolanaTokenProgram.ID.TOKEN.value, /* isSigner = */ false, /* isWritable = */ false),
        )
        val data = DISCRIMINATOR + BorshWriter()
            .u64(amount)
            .u64(feeAmount)
            .string(memo)
            .toByteArray()

        return Program.createTransactionInstruction(GachaVaultProgram.programId, keys, data)
    }
}