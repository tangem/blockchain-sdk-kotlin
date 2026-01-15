package com.tangem.blockchain.pendingtransactions.providers

import com.tangem.blockchain.common.network.providers.ProviderType

/**
 * Mapper for converting between [ProviderType] and storage keys.
 * Provides utilities for converting provider types
 */
internal class NetworkProviderMapper {

    /**
     * Converts a [ProviderType] to a string key suitable for storage.
     *
     * @param providerType The provider type to convert
     * @return A unique string key representing the provider type
     */
    fun toStorageKey(providerType: ProviderType): String {
        return when (providerType) {
            is ProviderType.Public -> "$PUBLIC_PROVIDER_PREFIX${providerType.url}"
            else -> providerType::class.qualifiedName ?: providerType.toString()
        }
    }

    /**
     * Finds a [ProviderType] by its storage key name.
     *
     * @param name The storage key name to search for
     * @param providerTypes Collection of provider types to search in
     * @return The matching [ProviderType] or null if not found
     */
    fun findProviderTypeByStorageKey(name: String, providerTypes: Collection<ProviderType>): ProviderType? {
        return providerTypes.firstOrNull { toStorageKey(it) == name }
    }

    /**
     * Checks if the provider is private.
     * Public providers have keys starting with "Public:" prefix.
     *
     * @param providerName The storage key name of the provider
     * @return true if the provider uses a private mempool, false otherwise
     */
    fun isPrivateProvider(providerName: String): Boolean {
        return !providerName.startsWith(PUBLIC_PROVIDER_PREFIX)
    }

    companion object {
        internal const val PUBLIC_PROVIDER_PREFIX = "Public:"
    }
}