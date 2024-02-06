package com.tangem.blockchain.common.datastorage.implementations

import android.util.Log
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.datastorage.BlockchainDataStorage
import com.tangem.blockchain.common.datastorage.BlockchainSavedData
import com.tangem.blockchain.network.moshi
import com.tangem.common.extensions.toCompressedPublicKey
import com.tangem.common.extensions.toHexString

/**
 * Advanced data storage that simplify [blockchainDataStorage] operations by using [moshi] under the hood
 *
 * @property blockchainDataStorage data storage
 *
 * @author Andrew Khokhlov on 02/02/2024
 */
internal class AdvancedDataStorage(
    private val blockchainDataStorage: BlockchainDataStorage,
) {

    /** Get data [T] by [publicKey] */
    suspend inline fun <reified T : BlockchainSavedData> getOrNull(publicKey: Wallet.PublicKey): T? {
        val uniqueKey = T::class.java.createKey(publicKey)
        val value = blockchainDataStorage.getOrNull(key = uniqueKey) ?: return null

        return fromJsonSafely(key = uniqueKey, value = value)
    }

    /**
     * Get data [T] by [key].
     * IMPORTANT! It's not recommended to use this method. Be careful with key, it MUST be unique.
     */
    suspend inline fun <reified T : BlockchainSavedData> getOrNull(key: String): T? {
        val value = blockchainDataStorage.getOrNull(key) ?: return null

        return fromJsonSafely(key = key, value = value)
    }

    /** Store [value] by [publicKey] */
    suspend inline fun <reified T : BlockchainSavedData> store(publicKey: Wallet.PublicKey, value: T) {
        val adapter = moshi.adapter(T::class.java)
        blockchainDataStorage.store(key = T::class.java.createKey(publicKey), value = adapter.toJson(value))
    }

    /**
     * Create a unique key by [publicKey] for [BlockchainSavedData].
     * Example, Hedera-7BD63F5DE1BF539525C33367592949AE9B99D518BF78F26F3904BCD30CFCF018
     */
    private inline fun <reified T : BlockchainSavedData> Class<T>.createKey(publicKey: Wallet.PublicKey): String {
        return "$simpleName-${publicKey.blockchainKey.toCompressedPublicKey().toHexString()}"
    }

    private inline fun <reified T : BlockchainSavedData> fromJsonSafely(key: String, value: String): T? {
        return runCatching { moshi.adapter(T::class.java).fromJson(value) }
            .fold(
                onSuccess = { it },
                onFailure = {
                    Log.e("BlockchainDataStorage", "Failed to parse data by key: $key")
                    null
                },
            )
    }
}
