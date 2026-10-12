package com.tangem.blockchain.collectibles.providers.solana.model

import com.tangem.blockchain.collectibles.providers.solana.GachaVaultProgram
import org.p2p.solanaj.core.PublicKey
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Decodes the borsh body of the program's `Config` account.
 *
 * Every field is fixed-width, so unlike [GachaVaultParser] this one needs no cursor and direct offsets are safe:
 *
 * ```
 *   8 admin(32)  40 usdc_mint(32)  72 gacha_wallet(32)  104 gacha_usdc_account(32)  136 fee_usdc_account(32)
 * 168 fee_bps(u16)  170 paused(bool)  171 _reserved(1)  172 allow_buyback_delegation(bool)  173 bump(u8)
 * ```
 *
 * Byte 171 is a legacy filler and must be skipped explicitly. It is not zero — on the live devnet account it holds a
 * 1 — so treating it as meaningful, or walking the tail relative to it, swaps [GachaConfig.isPaused] with
 * [GachaConfig.isBuybackDelegationAllowed]. The swap is silent, because today the two values are exactly 0 and 1.
 */
internal object GachaConfigParser {

    private val discriminator = GachaVaultProgram.discriminator(155, 12, 170, 224, 30, 250, 204, 130)

    private const val DISCRIMINATOR_SIZE = 8
    private const val PUBKEY_SIZE = 32
    private const val ACCOUNT_SIZE = 174
    private const val BYTE_MASK = 0xFF
    private const val U16_MASK = 0xFFFF

    /** Returns null when [data] is not a `Config` account of this program. */
    fun parse(data: ByteArray): GachaConfig? {
        if (data.size < ACCOUNT_SIZE) return null
        if (!data.copyOfRange(0, DISCRIMINATOR_SIZE).contentEquals(discriminator)) return null

        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(DISCRIMINATOR_SIZE)

        val admin = buffer.readPubkey()
        val usdcMint = buffer.readPubkey()
        val gachaWallet = buffer.readPubkey()
        val gachaUsdcAccount = buffer.readPubkey()
        val feeUsdcAccount = buffer.readPubkey()
        val feeBps = buffer.short.toInt() and U16_MASK
        val isPaused = buffer.readBoolean()
        buffer.get() // _reserved, legacy filler, holds 1 on devnet
        val isBuybackDelegationAllowed = buffer.readBoolean()
        val bump = buffer.get().toInt() and BYTE_MASK

        return GachaConfig(
            admin = admin,
            usdcMint = usdcMint,
            gachaWallet = gachaWallet,
            gachaUsdcAccount = gachaUsdcAccount,
            feeUsdcAccount = feeUsdcAccount,
            feeBps = feeBps,
            isPaused = isPaused,
            isBuybackDelegationAllowed = isBuybackDelegationAllowed,
            bump = bump,
        )
    }

    private fun ByteBuffer.readPubkey(): String {
        val bytes = ByteArray(PUBKEY_SIZE)
        get(bytes)
        return PublicKey(bytes).toBase58()
    }

    /** Borsh writes `0x00` or `0x01`; anything else is treated as set, so a corrupt `paused` fails closed. */
    private fun ByteBuffer.readBoolean(): Boolean = get().toInt() != 0
}