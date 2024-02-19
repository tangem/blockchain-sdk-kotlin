package com.tangem.blockchain.blockchains.algorand.network

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.blockchain.network.moshi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

internal class AlgorandNetworkProvider(override val baseUrl: String) : NetworkProvider {

    private val api: AlgorandApi = createRetrofitInstance(baseUrl).create(AlgorandApi::class.java)
    private val errorResponseAdapter = moshi.adapter(AlgorandErrorResponse::class.java)

    suspend fun getAccount(address: String): Result<AlgorandAccountResponse> {
        return try {
            val response = api.getAccount(address)
            Result.Success(response)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    suspend fun getTransactionParams(): Result<AlgorandTransactionParamsResponse> {
        return try {
            val response = api.getTransactionParams()
            Result.Success(response)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    suspend fun sendTransaction(data: ByteArray): Result<AlgorandTransactionResultResponse> {
        return try {
            val body = data.toRequestBody(contentType = APPLICATION_OCTET_STREAM.toMediaTypeOrNull())
            val response = api.commitTransaction(body = body)
            val responseBody = response.body()
            return if (response.isSuccessful && responseBody != null) {
                Result.Success(responseBody)
            } else {
                val errorBody =
                    response.errorBody()?.string() ?: return Result.Failure(BlockchainSdkError.FailedToSendException)
                val error = errorResponseAdapter.fromJson(errorBody)
                return Result.Failure(extractSendError(error))
            }
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    @Suppress("MagicNumber")
    suspend fun getPendingTransaction(txHash: String): Result<AlgorandPendingTransactionResponse?> {
        return try {
            val response = api.getPendingTransaction(transactionId = txHash)
            val result = if (response.code() == 404) null else response.body()
            Result.Success(result)
        } catch (e: Exception) {
            Result.Failure(e.toBlockchainSdkError())
        }
    }

    private fun extractSendError(errorResponse: AlgorandErrorResponse?): BlockchainSdkError {
        return if (errorResponse?.message != null) {
            BlockchainSdkError.Algorand.Send(errorResponse.message)
        } else {
            BlockchainSdkError.FailedToSendException
        }
    }

    private companion object {
        private const val APPLICATION_OCTET_STREAM = "application/octet-stream"
    }
}