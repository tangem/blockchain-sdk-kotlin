package com.tangem.blockchain.blockchains.cosmos.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CosmosApi {

    @GET("cosmos/auth/v1beta1/accounts/{address}")
    suspend fun getAccounts(@Path("address") address: String): Response<CosmosAccountResponse>

    @GET("cosmos/bank/v1beta1/balances/{address}")
    suspend fun getBalances(@Path("address") address: String): CosmosBalanceResponse

    @POST("cosmos/tx/v1beta1/simulate")
    suspend fun simulate(@Body simulateRequest: CosmosSendTransactionRequest): CosmosSimulateResponse

    @POST("cosmos/tx/v1beta1/txs")
    suspend fun txs(@Body simulateRequest: CosmosSendTransactionRequest): CosmosTxResponse

    @GET("cosmos/tx/v1beta1/txs/{hash}")
    suspend fun getTransactionStatus(@Path("hash") hash: String): CosmosTxResponse
}