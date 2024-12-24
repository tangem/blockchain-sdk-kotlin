package com.tangem.blockchain.network.jsonrpc

import com.tangem.blockchain.common.JsonRPCRequest
import com.tangem.blockchain.common.JsonRPCResponse
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

internal interface JsonRPCServiceApi {

    @Headers("Content-Type: application/json")
    @POST("/")
    suspend fun call(@Body jsonRPCRequest: JsonRPCRequest): JsonRPCResponse
}