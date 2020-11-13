package com.tangem.blockchain.blockchains.bitcoin.network.blockchaininfo

import okhttp3.ResponseBody
import retrofit2.http.*

interface BlockchainInfoApi {
    @GET("rawaddr/{address}")
    suspend fun getAddressData(
            @Path("address") address: String,
            @Query("offset") transactionOffset: Int?
    ): BlockchainInfoAddress

    @GET("unspent")
    suspend fun getUnspents(@Query("active") address: String): BlockchainInfoUnspents

    @GET("https://api.blockchain.info/mempool/fees")
    suspend fun getFees(): BlockchainInfoFees

    @FormUrlEncoded
    @POST("pushtx")
    suspend fun sendTransaction(@Field("tx") transaction: String): ResponseBody
}
