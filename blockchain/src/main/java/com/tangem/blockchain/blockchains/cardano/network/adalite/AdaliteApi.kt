package com.tangem.blockchain.blockchains.cardano.network.adalite

import retrofit2.http.*

interface AdaliteApi {
    @GET("api/addresses/summary/{address}")
    suspend fun getAddressData(@Path("address") address: String): AdaliteAddress

    @Headers("Content-Type: application/json")
    @POST("api/bulk/addresses/utxo")
    suspend fun getUnspents(@Body addresses: List<String>): AdaliteUnspents

    @Headers("Content-Type: application/json")
    @POST("api/v2/txs/signed")
    suspend fun sendTransaction(@Body adaliteBody: AdaliteSendBody): String // List<Any>?
}
