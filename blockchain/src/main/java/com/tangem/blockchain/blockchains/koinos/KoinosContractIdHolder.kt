package com.tangem.blockchain.blockchains.koinos

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr

internal class KoinosContractIdHolder(
    isTestnet: Boolean,
    private val loadKoinContractId: suspend () -> Result<String>,
) {

    private var koinContractId: String? = null
    private val mutex = Mutex()
    private val fallbackContractId: String = if (isTestnet) FALLBACK_CONTRACT_ID_TESTNET else FALLBACK_CONTRACT_ID

    suspend fun get(): String {
        koinContractId?.let { return it }

        return mutex.withLock {
            koinContractId?.let { return it }

            val result = loadKoinContractId()
            if (result is Result.Success) {
                koinContractId = result.data
            }
            result.successOr { fallbackContractId }
        }
    }

    private companion object {
        const val FALLBACK_CONTRACT_ID: String = "19GYjDBVXU7keLbYvMLazsGQn3GTWHjHkK"
        const val FALLBACK_CONTRACT_ID_TESTNET: String = "1EdLyQ67LW6HVU1dWoceP4firtyz77e37Y"
    }
}