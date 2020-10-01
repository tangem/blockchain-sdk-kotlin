package com.tangem.blockchain.network.blockcypher

import com.squareup.moshi.JsonClass
import retrofit2.http.*

interface BlockcypherApi {
    @GET("v1/{blockchain}/{network}")
    suspend fun getFee(
            @Path("blockchain") blockchain: String,
            @Path("network") network: String,
            @Query("token") token: String? = null
    ): BlockcypherFee

    @GET("v1/{blockchain}/{network}/addrs/{address}?includeScript=true")
    suspend fun getAddressData(
            @Path("blockchain") blockchain: String,
            @Path("network") network: String,
            @Path("address") address: String,
            @Query("limit") limit: Int? = null,
            @Query("token") token: String? = null
    ): BlockcypherAddress

    @Headers("Content-Type: application/json")
    @POST("v1/{blockchain}/{network}/txs/push")
    suspend fun sendTransaction(
            @Path("blockchain") blockchain: String,
            @Path("network") network: String,
            @Body body: BlockcypherSendBody,
            @Query("token") token: String
    ): BlockcypherTx
}

@JsonClass(generateAdapter = true)
data class BlockcypherSendBody(val tx: String)