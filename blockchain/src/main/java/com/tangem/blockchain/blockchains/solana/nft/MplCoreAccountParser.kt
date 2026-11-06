package com.tangem.blockchain.blockchains.solana.nft

import com.tangem.blockchain.blockchains.solana.alt.borsh.SolanaBorshException
import com.tangem.blockchain.blockchains.solana.alt.borsh.SolanaBorshReader
import org.p2p.solanaj.core.PublicKey

internal enum class MplCoreTransferBlock { Frozen, ExternalCheck }

internal data class MplCoreAsset(
    val owner: PublicKey,
    val collection: PublicKey?,
    val transferBlock: MplCoreTransferBlock?,
)

/**
 * Reads Core `AssetV1` / `CollectionV1` accounts and their plugin registry.
 *
 * The program rejects every plugin refusal with the same `InvalidAuthority`, so a frozen asset can only be told
 * apart before signing, from its plugins. An asset plugin overrides the same plugin type of its collection, but a
 * frozen collection still blocks every asset in it, so both accounts are checked.
 */
internal object MplCoreAccountParser {

    private const val ASSET_V1 = 1
    private const val PLUGIN_HEADER_V1 = 3
    private const val PLUGIN_REGISTRY_V1 = 4
    private const val COLLECTION_V1 = 5

    private const val UPDATE_AUTHORITY_NONE = 0
    private const val UPDATE_AUTHORITY_ADDRESS = 1
    private const val UPDATE_AUTHORITY_COLLECTION = 2
    private const val AUTHORITY_ADDRESS = 3

    private const val COLLECTION_COUNTERS_SIZE = 2 * Int.SIZE_BYTES
    private const val TRANSFER_EVENT = 1
    private val FREEZE_PLUGINS = setOf(1, 5)

    fun parseAsset(data: ByteArray): MplCoreAsset? = runCatching {
        val reader = SolanaBorshReader(data)
        if (reader.u8() != ASSET_V1) throw SolanaBorshException("Not an AssetV1 account")

        val owner = reader.publicKey()
        val collection = when (reader.u8()) {
            UPDATE_AUTHORITY_NONE -> null
            UPDATE_AUTHORITY_ADDRESS -> {
                reader.skip(SolanaBorshReader.PUBLIC_KEY_SIZE)
                null
            }
            UPDATE_AUTHORITY_COLLECTION -> reader.publicKey()
            else -> throw SolanaBorshException("Unknown update authority")
        }
        reader.skipString()
        reader.skipString()
        reader.option { it.u64() }

        MplCoreAsset(owner = owner, collection = collection, transferBlock = transferBlock(reader))
    }.getOrNull()

    fun parseCollectionTransferBlock(data: ByteArray): Result<MplCoreTransferBlock?> = runCatching {
        val reader = SolanaBorshReader(data)
        if (reader.u8() != COLLECTION_V1) throw SolanaBorshException("Not a CollectionV1 account")

        reader.skip(SolanaBorshReader.PUBLIC_KEY_SIZE)
        reader.skipString()
        reader.skipString()
        reader.skip(COLLECTION_COUNTERS_SIZE)

        transferBlock(reader)
    }

    private fun transferBlock(reader: SolanaBorshReader): MplCoreTransferBlock? {
        if (reader.isAtEnd) return null
        if (reader.u8() != PLUGIN_HEADER_V1) throw SolanaBorshException("Unknown plugin header")

        val registry = reader.at(reader.offset())
        if (registry.u8() != PLUGIN_REGISTRY_V1) throw SolanaBorshException("Unknown plugin registry")

        val freezePluginOffsets = mutableListOf<Int>()
        registry.vector { record ->
            val type = record.u8()
            record.skipAuthority()
            val offset = record.offset()
            if (type in FREEZE_PLUGINS) freezePluginOffsets += offset
        }

        var hasExternalTransferCheck = false
        registry.vector { record ->
            record.skip(1)
            record.skipAuthority()
            val isTransferChecked = record.option { checks ->
                var isTransferEvent = false
                checks.vector { check ->
                    if (check.u8() == TRANSFER_EVENT) isTransferEvent = true
                    check.skip(Int.SIZE_BYTES)
                }
                isTransferEvent
            }
            if (isTransferChecked == true) hasExternalTransferCheck = true
            record.skip(Long.SIZE_BYTES)
            record.option { it.u64() }
            record.option { it.u64() }
        }

        val isFrozen = freezePluginOffsets.any { offset ->
            val plugin = reader.at(offset)
            if (plugin.u8() !in FREEZE_PLUGINS) throw SolanaBorshException("Plugin offset points elsewhere")
            plugin.bool()
        }

        return when {
            isFrozen -> MplCoreTransferBlock.Frozen
            hasExternalTransferCheck -> MplCoreTransferBlock.ExternalCheck
            else -> null
        }
    }

    private fun SolanaBorshReader.skipAuthority() {
        if (u8() == AUTHORITY_ADDRESS) skip(SolanaBorshReader.PUBLIC_KEY_SIZE)
    }
}