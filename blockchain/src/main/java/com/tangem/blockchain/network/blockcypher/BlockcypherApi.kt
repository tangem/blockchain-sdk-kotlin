package com.tangem.blockchain.network.blockcypher

import com.squareup.moshi.JsonClass
import retrofit2.http.*

interface BlockcypherApi {
    @GET("./")
    suspend fun getFee(
            @Query("token") token: String? = null
    ): BlockcypherFee

    @GET("addrs/{address}?includeScript=true")
    suspend fun getAddressData(
            @Path("address") address: String,
            @Query("limit") limit: Int? = null,
            @Query("token") token: String? = null
    ): BlockcypherAddress

    @Headers("Content-Type: application/json")
    @POST("txs/push")
    suspend fun sendTransaction(
            @Body body: BlockcypherSendBody,
            @Query("token") token: String
    ): BlockcypherRawTx

    @GET("txs/{hash}")
    suspend fun getTransaction(
            @Path("hash") transactionHash: String,
            @Query("token") token: String? = null
    ): BlockcypherTransaction
}

@JsonClass(generateAdapter = true)
data class BlockcypherSendBody(val tx: String)