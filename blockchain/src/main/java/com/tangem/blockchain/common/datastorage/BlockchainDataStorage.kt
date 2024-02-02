package com.tangem.blockchain.common.datastorage

/**
 * Data storage. Stores data in JSON format.
 *
 * @author Andrew Khokhlov on 02/02/2024
 */
interface BlockchainDataStorage {

    /** Get data in JSON format by [key] */
    suspend fun getOrNull(key: String): String?

    /** Store [value] in JSON format by [key] */
    suspend fun store(key: String, value: String)
}
