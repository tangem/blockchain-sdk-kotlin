package com.tangem.blockchain.blockchains.polkadot.network

import com.tangem.blockchain.common.JsonRPCRequest
import com.tangem.blockchain.common.JsonRPCResponse
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

internal interface PolkadotApi {

    @Headers("Content-Type: application/json")
    @POST(".")
    suspend fun post(@Body body: JsonRPCRequest): JsonRPCResponse
}