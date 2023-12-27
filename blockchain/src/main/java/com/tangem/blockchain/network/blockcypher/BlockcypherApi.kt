package com.tangem.blockchain.network.blockcypher

import com.squareup.moshi.JsonClass
import com.tangem.blockchain.network.blockcypher.response.SendTransactionResponse
import retrofit2.http.*

interface BlockcypherApi {
    @GET("./")
    suspend fun getFee(@Query("token") token: String? = null): BlockcypherFee

    @GET("addrs/{address}?includeScript=true")
    suspend fun getAddressData(
        @Path("address") address: String,
        @Query("limit") limit: Int? = null,
        @Query("token") token: String? = null,
    ): BlockcypherAddress

    @Headers("Content-Type: application/json")
    @POST("txs/push")
    suspend fun sendTransaction(@Body body: BlockcypherSendBody, @Query("token") token: String): SendTransactionResponse
}

@JsonClass(generateAdapter = true)
data class BlockcypherSendBody(val tx: String)
