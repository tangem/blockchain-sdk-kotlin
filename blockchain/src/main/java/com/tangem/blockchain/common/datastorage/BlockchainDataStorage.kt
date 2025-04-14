package com.tangem.blockchain.common.datastorage

/**
 * Data storage. Stores data in JSON format.
 *
[REDACTED_AUTHOR]
 */
interface BlockchainDataStorage {

    /** Get data in JSON format by [key] */
    suspend fun getOrNull(key: String): String?

    /** Store [value] in JSON format by [key] */
    suspend fun store(key: String, value: String)

    /** Remove [value] from storage by [key] */
    suspend fun remove(key: String)
}