package com.tangem.blockchain.blockchains.solana.nft

import org.p2p.solanaj.core.PublicKey
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Builds Core `AssetV1` / `CollectionV1` accounts with a plugin registry, laid out as the program writes them. */
internal object MplCoreAccountBytes {

    data class FreezePlugin(val type: Int, val frozen: Boolean)

    const val FREEZE_DELEGATE = 1
    const val PERMANENT_FREEZE_DELEGATE = 5

    private const val PLUGIN_HEADER_V1 = 3
    private const val PLUGIN_REGISTRY_V1 = 4
    private const val OWNER_AUTHORITY = 1
    private const val ORACLE = 1
    private const val TRANSFER_EVENT = 1
    private const val CAN_REJECT = 4

    fun asset(
        owner: PublicKey,
        collection: PublicKey?,
        freezePlugins: List<FreezePlugin> = emptyList(),
        externalTransferCheck: Boolean = false,
    ): ByteArray {
        val out = Borsh().u8(1).bytes(owner.toByteArray())
        if (collection == null) out.u8(0) else out.u8(2).bytes(collection.toByteArray())
        out.string("Card").string("https://example.com/card.json").u8(0)
        return out.withPlugins(freezePlugins, externalTransferCheck)
    }

    fun collection(
        updateAuthority: PublicKey,
        freezePlugins: List<FreezePlugin> = emptyList(),
        externalTransferCheck: Boolean = false,
    ): ByteArray = Borsh().u8(5).bytes(updateAuthority.toByteArray())
        .string("Collection").string("https://example.com/collection.json").u32(1).u32(1)
        .withPlugins(freezePlugins, externalTransferCheck)

    private fun Borsh.withPlugins(plugins: List<FreezePlugin>, externalTransferCheck: Boolean): ByteArray {
        if (plugins.isEmpty() && !externalTransferCheck) return toByteArray()

        val firstPluginOffset = size + 1 + Long.SIZE_BYTES
        val registryOffset = firstPluginOffset + plugins.size * 2
        u8(PLUGIN_HEADER_V1).u64(registryOffset.toLong())
        plugins.forEach { u8(it.type).u8(if (it.frozen) 1 else 0) }

        u8(PLUGIN_REGISTRY_V1).u32(plugins.size)
        plugins.forEachIndexed { index, plugin ->
            u8(plugin.type).u8(OWNER_AUTHORITY).u64((firstPluginOffset + index * 2).toLong())
        }
        if (externalTransferCheck) {
            u32(1).u8(ORACLE).u8(OWNER_AUTHORITY)
                .u8(1).u32(1).u8(TRANSFER_EVENT).u32(CAN_REJECT)
                .u64(0).u8(0).u8(0)
        } else {
            u32(0)
        }
        return toByteArray()
    }

    private class Borsh {
        private val out = ByteArrayOutputStream()
        val size: Int get() = out.size()

        fun u8(value: Int) = apply { out.write(value) }
        fun bytes(value: ByteArray) = apply { out.write(value) }
        fun u32(value: Int) = apply {
            out.write(ByteBuffer.allocate(Int.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array())
        }
        fun u64(value: Long) = apply {
            out.write(ByteBuffer.allocate(Long.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array())
        }
        fun string(value: String) = apply {
            val encoded = value.toByteArray()
            u32(encoded.size).bytes(encoded)
        }
        fun toByteArray(): ByteArray = out.toByteArray()
    }
}