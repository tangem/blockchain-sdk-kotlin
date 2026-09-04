package com.tangem.blockchain.collectibles.providers.solana

import com.tangem.blockchain.blockchains.solana.solanaj.core.createAssociatedSolanaTokenAddress
import com.tangem.blockchain.blockchains.solana.solanaj.program.SolanaTokenProgram
import org.p2p.solanaj.core.PublicKey
import org.p2p.solanaj.programs.Program

/**
 * Addresses and derivations of the `tangem_gacha_vault` Anchor program.
 *
 * Seeds, account layouts and discriminators follow the program IDL, which is regenerated on every redeploy and is
 * not published on chain. After a redeploy the derivations here have to be re-checked against the current IDL.
 */
internal object GachaVaultProgram {

    val programId = PublicKey("29agFEruMu2jedVwnDgKPuq7ejmTiB7sEqzdhufQQS9u")

    val systemProgramId: PublicKey = Program.Id.system

    private val configSeed = "config".toByteArray()
    private val vaultSeed = "vault".toByteArray()

    /** `PDA(["config"], programId)`. On devnet resolves to `53EDbBLB1sQuVv6aH7us9TQ7XSGXFijPmPdAGNcY8Y1d`. */
    fun configAddress(): Result<PublicKey> = runCatching {
        PublicKey.findProgramAddress(listOf(configSeed), programId).address
    }

    /** `PDA(["vault", cold_owner], programId)`. The vault address depends on the cold key only. */
    fun vaultAddress(coldOwner: PublicKey): Result<PublicKey> = runCatching {
        PublicKey.findProgramAddress(listOf(vaultSeed, coldOwner.toByteArray()), programId).address
    }

    /**
     * `PDA([vault, SPL Token program, usdc_mint], ATokenGPvb...)` — the vault's USDC token account.
     *
     * The vault PDA is off the ed25519 curve, so the address needs a plain `findProgramAddress` with no on-curve
     * check — which is what [createAssociatedSolanaTokenAddress] does.
     *
     * Legacy [SolanaTokenProgram.ID.TOKEN], not `TOKEN_2022`: the program requires a legacy mint in its config,
     * otherwise `6021 UnsupportedMint`. Being a seed, the wrong one silently yields a different address, not an error.
     */
    fun vaultUsdcAddress(vault: PublicKey, usdcMint: PublicKey): Result<PublicKey> = createAssociatedSolanaTokenAddress(
        account = vault,
        mint = usdcMint,
        tokenProgramId = SolanaTokenProgram.ID.TOKEN,
    )

    /**
     * Builds an 8-byte Anchor discriminator from the numbers as they appear in the IDL, so the literals can be
     * compared with the JSON without converting to signed bytes by hand.
     */
    fun discriminator(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }
}