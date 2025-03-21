package com.tangem.blockchain.blockchains.koinos

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.tangem.blockchain.extensions.Result

class KoinosContractIdHolder(
    private val loadContractId: suspend () -> Result<String>,
) {
    @Volatile
    private var contractId: String? = null
    private val mutex = Mutex()

    suspend fun get(): Result<String> {
        contractId?.let { return Result.Success(it) }

        return mutex.withLock {
            contractId?.let { return Result.Success(it) }

            val result = loadContractId()
            if (result is Result.Success) {
                contractId = result.data
            }
            result
        }
    }
}