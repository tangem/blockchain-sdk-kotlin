package com.tangem.blockchain.blockchains.polkadot

import com.tangem.blockchain.extensions.Result
import io.emeraldpay.polkaj.api.PolkadotApi
import io.emeraldpay.polkaj.apihttp.PolkadotHttpApi
import io.emeraldpay.polkaj.types.Address
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
[REDACTED_AUTHOR]
 */
class PolkadotNetworkService(
    private val provider: PolkadotApi
) {
    init {
        val api = PolkadotHttpApi.Builder().
    }

    suspend fun getMainAccountInfo(account: Address): Result<Any> = withContext(Dispatchers.IO) {
    }

    suspend fun accountInfo(account: Address): Result<Any> = withContext(Dispatchers.IO) {
    }

    suspend fun getFees(): Result<Any> = withContext(Dispatchers.IO) {
    }

    suspend fun sendTransaction(): Result<String> = withContext(Dispatchers.IO) {
    }

    private suspend fun getTransactionsInProgressInfo(): Result<Any> = withContext(Dispatchers.IO) {
    }
}