package com.tangem.blockchain.blockchains.cosmos.network

import android.util.Log
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.blockchain.network.moshi
import retrofit2.Response
import retrofit2.create

class CosmosRestProvider(
    val host: String,
) {

    private val api = createRetrofitInstance(host).create<CosmosApi>()
    private val errorAdapter = moshi.adapter(CosmosErrorResponse::class.java)
    private val sendRequestAdapter = moshi.adapter(CosmosSendTransactionRequest::class.java)

    suspend fun accounts(address: String): Result<CosmosAccountResponse?> {
        return when (val accountsResult = parseAccountsResponse(api.getAccounts(address))) {
            is Result.Success -> accountsResult
            is Result.Failure -> {
                if (accountsResult.error is BlockchainSdkError.Cosmos.Api && accountsResult.error.code == EMPTY_ACCOUNT) {
                    Result.Success(null)
                } else {
                    accountsResult
                }
            }
        }
    }

    suspend fun balances(address: String): Result<CosmosBalanceResponse> {
        return try {
            val balances = api.getBalances(address)
            Result.Success(balances)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    suspend fun simulate(requestBody: String): Result<CosmosSimulateResponse> {
        return try {
            val request = requireNotNull(sendRequestAdapter.fromJson(requestBody)) { FAILED_TO_PARSE_JSON }
            val response = api.simulate(request)
            Result.Success(response)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    suspend fun txs(requestBody: String): Result<CosmosTxResponse> {
        return try {
            val request = requireNotNull(sendRequestAdapter.fromJson(requestBody)) { FAILED_TO_PARSE_JSON }
            val response = api.txs(request)
            Result.Success(response)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private fun parseAccountsResponse(response: Response<CosmosAccountResponse>): Result<CosmosAccountResponse?> {
        if (response.isSuccessful) return Result.Success(response.body())
        val errorBody = response.errorBody()?.string()

        val cosmosError = try {
            errorBody?.let { errorAdapter.fromJson(it) }
        } catch (e: Exception) {
            Log.e(this::class.java.simpleName, e.message, e)
            null
        }

        return if (cosmosError != null) {
            Result.Failure(BlockchainSdkError.Cosmos.Api(cosmosError.code, cosmosError.message))
        } else {
            Result.Failure(BlockchainSdkError.AccountNotFound)
        }
    }
}

private const val FAILED_TO_PARSE_JSON = "Failed to parse JSON"
private const val EMPTY_ACCOUNT = 5