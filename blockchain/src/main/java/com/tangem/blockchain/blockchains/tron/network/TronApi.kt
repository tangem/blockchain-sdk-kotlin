package com.tangem.blockchain.blockchains.tron.network

import retrofit2.http.*

interface TronApi {

    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST("wallet/getaccount")
    suspend fun getAccount(
        @Body requestBody: TronGetAccountRequest,
    ): TronGetAccountResponse

    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST("wallet/getaccountresource")
    suspend fun getAccountResource(
        @Body requestBody: TronGetAccountRequest,
    ): TronGetAccountResourceResponse

    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST("wallet/getnowblock")
    suspend fun getNowBlock(): TronBlock

    @Headers("Accept: application/json")
    @POST("wallet/broadcasthex")
    suspend fun broadcastHex(@Body requestBody: TronBroadcastRequest): TronBroadcastResponse

    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST("wallet/triggerconstantcontract")
    suspend fun getTokenBalance(@Body requestBody: TronTriggerSmartContractRequest): TronTriggerSmartContractResponse

    @Headers("Content-Type: application/json", "Accept: application/json")
    @GET("v1/contracts/{contractAddress}/transactions")
    suspend fun getTokenTransactionHistory(
        @Path("contractAddress") contractAddress: String,
        @Query("only_confirmed") onlyConfirmed: Boolean = true,
        @Query("limit") limit: Int = 50,
    ): TronTokenHistoryResponse

    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST("walletsolidity/gettransactioninfobyid")
    suspend fun getTransactionInfoById(@Body requestBody: TronTransactionInfoRequest): TronTransactionInfoResponse

    @Headers("Content-Type: application/json", "Accept: application/json")
    @GET("wallet/getchainparameters")
    suspend fun getChainParameters(): TronChainParametersResponse
}