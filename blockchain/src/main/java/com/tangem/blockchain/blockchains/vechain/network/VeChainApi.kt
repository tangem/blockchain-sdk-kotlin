package com.tangem.blockchain.blockchains.vechain.network

import retrofit2.Response
import retrofit2.http.*

internal interface VeChainApi {

    @GET("accounts/{address}")
    suspend fun getAccount(@Path("address") address: String): VeChainGetAccountResponse

    @POST("accounts/*")
    suspend fun callContract(@Body requestBody: VeChainContractCallRequest): List<VeChainContractCallResponse>

    @GET("blocks/best")
    suspend fun getLatestBlockInfo(): VeChainLatestBlockResponse

    @POST("transactions")
    suspend fun commitTransaction(@Body body: VeChainCommitTransactionRequest): VeChainCommitTransactionResponse

    @GET("transactions/{id}")
    suspend fun getTransactionInfo(
        @Path("id") transactionId: String,
        @Query("pending") pending: Boolean,
    ): Response<VeChainTransactionInfoResponse?>
}
