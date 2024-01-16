package com.tangem.blockchain.blockchains.vechain.network

import retrofit2.Response
import retrofit2.http.*

internal interface VechainApi {

    @GET("accounts/{address}")
    suspend fun getAccount(@Path("address") address: String): VechainGetAccountResponse

    @POST("accounts/*")
    suspend fun getTokenBalance(@Body requestBody: VechainTokenBalanceRequest): List<VechainData>

    @GET("blocks/best")
    suspend fun getLatestBlockInfo(): VechainLatestBlockResponse

    @POST("transactions")
    suspend fun commitTransaction(@Body body: VechainCommitTransactionRequest): VechainCommitTransactionResponse

    @GET("transactions/{id}")
    suspend fun getTransactionInfo(
        @Path("id") transactionId: String,
        @Query("pending") pending: Boolean,
    ): Response<VechainTransactionInfoResponse?>
}