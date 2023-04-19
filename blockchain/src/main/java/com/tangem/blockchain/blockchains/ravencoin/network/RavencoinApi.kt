package com.tangem.blockchain.blockchains.ravencoin.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.math.BigDecimal

private const val USER_AGENT_HEADER = "User-Agent: Mozilla/5.0 Version/16.1 Safari/605.1.15"

interface RavencoinApi {

    @Headers(USER_AGENT_HEADER)
    @GET("addr/{address}")
    suspend fun getWalletInfo(@Path("address") address: String, ): RavencoinWalletInfoResponse

    @Headers(USER_AGENT_HEADER)
    @GET("addr/{address}/utxo")
    suspend fun getUTXO(@Path("address") address: String) : List<RavencoinWalletUTXOResponse>

    @Headers(USER_AGENT_HEADER)
    @GET("utils/estimatesmartfee")
    suspend fun getFee(
        @Query("nbBlocks") numberOfBlocks: Int,
        @Query("mode") mode: String = "economical",
    ): Map<String, BigDecimal>

    @Headers(USER_AGENT_HEADER)
    @GET("txs")
    suspend fun getTransactions(
        @Query("address") address: String,
        @Query("pageNum") pageNumber: Int = 0,
    ) : RavencoinTransactionHistoryResponse

    @Headers(USER_AGENT_HEADER)
    @POST("tx/send")
    suspend fun send(@Body body: RavencoinRawTransactionRequest): RavencoinRawTransactionResponse

}