package com.tangem.demo.datastorage

import com.tangem.blockchain.common.datastorage.BlockchainDataStorage

internal object DummyBlockchainDataStorage : BlockchainDataStorage {
    override suspend fun getOrNull(key: String): String? = null
    override suspend fun store(key: String, value: String) = Unit
}
