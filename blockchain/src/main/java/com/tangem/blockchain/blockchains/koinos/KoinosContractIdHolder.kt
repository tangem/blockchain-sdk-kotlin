package com.tangem.blockchain.blockchains.koinos

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.tangem.blockchain.extensions.Result

internal class KoinosContractIdHolder(private val loadKoinContractId: suspend () -> Result<String>) {

    @Volatile
    private var koinContractId: String? = null
    private val mutex = Mutex()

    suspend fun get(): Result<String> {
        koinContractId?.let { return Result.Success(it) }

        return mutex.withLock {
            koinContractId?.let { return Result.Success(it) }

            val result = loadKoinContractId()
            if (result is Result.Success) {
                koinContractId = result.data
            }
            result
        }
    }
}