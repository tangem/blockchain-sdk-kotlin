package com.tangem.blockchain.network.blockscout

import com.tangem.blockchain.common.BlockscoutCredentials
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.BasicAuthInterceptor
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.successOr
import com.tangem.blockchain.network.API_BLOCKSCOUT_BICOCCACHAIN
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.common.services.retryIO

/**
[REDACTED_AUTHOR]
 */
class BlockscoutNetworkProvider(
    private val credentials: BlockscoutCredentials?,
) {
    private val api = createApi(credentials)

    private fun createApi(credentials: BlockscoutCredentials?): BlockscoutApi {
        val authInterceptor = credentials?.let { BasicAuthInterceptor(it.userName, it.password) }
        return createRetrofitInstance(
            baseUrl = API_BLOCKSCOUT_BICOCCACHAIN,
            headerInterceptors = listOfNotNull(authInterceptor)
        ).create(BlockscoutApi::class.java)
    }

    suspend fun getTransactionsList(address: String): Result<List<BlockscoutTransaction>> {
        return processGetTransactionResponse(address) {
            performRequest { api.getTransactionsList(address) }
        }
    }

    suspend fun getTokenTransactionsList(address: String): Result<List<BlockscoutTransaction>> {
        return processGetTransactionResponse(address) {
            performRequest { api.getTokenTransactionsList(address) }
        }
    }

    private suspend fun processGetTransactionResponse(
        address: String,
        method: suspend (String) -> Result<BlockscoutResponse<List<BlockscoutTransaction>>>,
    ): Result<List<BlockscoutTransaction>> {
        val response = method(address).successOr { return it }
        return Result.Success(response.result ?: emptyList())
    }

    private suspend fun <T> performRequest(block: suspend () -> T): Result<T> {
        return try {
            val result = retryIO { block() }
            Result.Success(result)
        } catch (ex: Exception) {
            Result.Failure(ex.toBlockchainSdkError())
        }
    }
}