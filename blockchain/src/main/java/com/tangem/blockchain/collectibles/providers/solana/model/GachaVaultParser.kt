package com.tangem.blockchain.collectibles.providers.solana.model

import com.tangem.blockchain.collectibles.models.CollectiblesVault
import com.tangem.blockchain.collectibles.providers.solana.GachaVaultProgram
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.p2p.solanaj.core.PublicKey

/**
 * Decodes the borsh body of a `Vault` account.
 *
 * Offsets are fixed only up to byte 112. After that the two `Option<Pubkey>` fields occupy 1 byte when `None` and 33
 * when `Some`, so the tail must be walked with a cursor. Reading it at fixed offsets is not merely imprecise: the
 * unwritten tail of the account holds stale bytes from earlier writes rather than zeros, so a fixed-offset reader can
 * report a buyback that does not exist.
 */
internal object GachaVaultParser {

    private val discriminator = GachaVaultProgram.discriminator(211, 8, 232, 43, 2, 152, 117, 119)

    private const val DISCRIMINATOR_SIZE = 8
    private const val PUBKEY_SIZE = 32
    private const val TAIL_OFFSET = 112
    private const val SOME_TAG = 1

    /** Returns null when [data] is not a `Vault` account of this program. */
    fun parse(address: PublicKey, data: ByteArray): CollectiblesVault? {
        if (data.size <= TAIL_OFFSET) return null
        if (!data.copyOfRange(0, DISCRIMINATOR_SIZE).contentEquals(discriminator)) return null

        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(DISCRIMINATOR_SIZE)

        val coldOwner = buffer.readPubkey()
        val hotDelegate = buffer.readPubkey()
        val perSpinCap = buffer.long
        val dailyCap = buffer.long
        val spentToday = buffer.long
        val dayIndex = buffer.long
        buffer.long // _reserved, legacy filler

        var cursor = TAIL_OFFSET
        val liveBuybackMint = data.readOptionalPubkey(cursor) ?: return null
        cursor += liveBuybackMint.size
        val liveBuybackToken = data.readOptionalPubkey(cursor) ?: return null
        cursor += liveBuybackToken.size
        if (cursor >= data.size) return null

        return CollectiblesVault(
            address = address.toBase58(),
            coldOwner = coldOwner,
            hotDelegate = hotDelegate,
            perSpinCap = perSpinCap,
            dailyCap = dailyCap,
            spentToday = spentToday,
            dayIndex = dayIndex,
            liveBuybackMint = liveBuybackMint.value,
            liveBuybackToken = liveBuybackToken.value,
        )
    }

    private fun ByteBuffer.readPubkey(): String {
        val bytes = ByteArray(PUBKEY_SIZE)
        get(bytes)
        return PublicKey(bytes).toBase58()
    }

    private fun ByteArray.readOptionalPubkey(offset: Int): OptionalPubkey? {
        val tag = getOrNull(offset)?.toInt() ?: return null
        if (tag != SOME_TAG) return OptionalPubkey(value = null, size = 1)

        val end = offset + 1 + PUBKEY_SIZE
        if (end > size) return null

        return OptionalPubkey(
            value = PublicKey(copyOfRange(offset + 1, end)).toBase58(),
            size = 1 + PUBKEY_SIZE,
        )
    }

    private data class OptionalPubkey(val value: String?, val size: Int)
}