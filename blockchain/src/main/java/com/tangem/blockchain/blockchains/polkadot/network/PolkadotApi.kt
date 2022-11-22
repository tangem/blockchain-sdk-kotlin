package com.tangem.blockchain.blockchains.polkadot.network

import retrofit2.http.*

interface PolkadotApi {
    @Headers("Content-Type: application/json")
    @POST(".")
    suspend fun post(@Body body: PolkadotBody?): PolkadotResponse
}