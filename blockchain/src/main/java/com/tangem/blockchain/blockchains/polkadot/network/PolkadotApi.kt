package com.tangem.blockchain.blockchains.polkadot.network

import retrofit2.http.*

interface PolkadotApi {
    @Headers("Content-Type: application/json")
    @POST(".")
    suspend fun post(@Body body: RpcBody?): RpcMapResponse

    @Headers("Content-Type: application/json")
    @POST(".")
    suspend fun postWithStringResult(@Body body: RpcBody?): RpcStringResponse
}