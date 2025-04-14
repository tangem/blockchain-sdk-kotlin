package com.tangem.blockchain.blockchains.algorand.network

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

internal interface AlgorandApi {

    @GET("v2/accounts/{address}")
    suspend fun getAccount(@Path("address") address: String): AlgorandAccountResponse

    @GET("v2/transactions/params")
    suspend fun getTransactionParams(): AlgorandTransactionParamsResponse

    @POST("v2/transactions")
    suspend fun commitTransaction(
        @Body body: RequestBody,
        @Header("Content-Type") contentType: String = X_BINARY_HEADER,
    ): Response<AlgorandTransactionResultResponse>

    @GET("v2/transactions/pending/{txid}")
    suspend fun getPendingTransaction(
        @Path("txid") transactionId: String,
    ): Response<AlgorandPendingTransactionResponse?>

    companion object {
        internal const val X_BINARY_HEADER = "application/x-binary"
    }
}