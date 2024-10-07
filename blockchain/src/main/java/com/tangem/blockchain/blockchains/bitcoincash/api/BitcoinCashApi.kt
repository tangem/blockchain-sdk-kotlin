package com.tangem.blockchain.blockchains.bitcoincash.api

import com.tangem.blockchain.common.JsonRPCRequest
import com.tangem.blockchain.common.JsonRPCResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

internal interface BitcoinCashApi {

    @Headers("Content-Type: application/json")
    @POST("/")
    suspend fun getFee(@Body request: JsonRPCRequest): Response<JsonRPCResponse>

    @Headers("Content-Type: application/json")
    @POST("/")
    suspend fun sendTransaction(@Body request: JsonRPCRequest): Response<JsonRPCResponse>
}