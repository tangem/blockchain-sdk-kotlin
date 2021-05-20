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

    @FormUrlEncoded
    @POST("pushtx")
    suspend fun sendTransaction(@Field("tx") transaction: String): ResponseBody

    @GET("rawtx/{transaction}")
    suspend fun getTransaction(
            @Path("transaction") transaction: String
    ): BlockchainInfoTransaction
}

