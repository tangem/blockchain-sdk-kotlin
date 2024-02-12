package com.tangem.blockchain.blockchains.algorand.network

import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.createRetrofitInstance
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

internal class AlgorandNetworkProvider(override val baseUrl: String) : NetworkProvider {

    private val api: AlgorandApi = createRetrofitInstance(baseUrl).create(AlgorandApi::class.java)

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
            Result.Success(response)
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

    private companion object {
        private const val APPLICATION_OCTET_STREAM = "application/octet-stream"
    }
}