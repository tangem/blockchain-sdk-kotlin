package com.tangem.blockchain.transactionhistory.blockchains.polygon.network

import retrofit2.http.GET
import retrofit2.http.Query

internal interface EtherScanApi {

    @GET("api")
    suspend fun getCoinTransactionHistory(
        @Query("chainid") chainId: Int,
        @Query("address") address: String,
        @Query("page") page: Int,
        @Query("offset") offset: Int,
        @Query("apikey") apiKey: String,
        @Query("action") action: String = "txlist",
        @Query("module") module: String = "account",
        @Query("startblock") startBlock: Int = 0,
        @Query("endblock") endBlock: Long = Long.MAX_VALUE,
        @Query("sort") sort: String = "desc",
    ): PolygonTransactionHistoryResponse

    @Suppress("LongParameterList")
    @GET("api")
    suspend fun getTokenTransactionHistory(
        @Query("chainid") chainId: Int,
        @Query("address") address: String,
        @Query("page") page: Int,
        @Query("offset") offset: Int,
        @Query("apikey") apiKey: String,
        @Query("action") action: String = "tokentx",
        @Query("contractAddress") contractAddress: String?,
        @Query("module") module: String = "account",
        @Query("startblock") startBlock: Int = 0,
        @Query("endblock") endBlock: Long = Long.MAX_VALUE,
        @Query("sort") sort: String = "desc",
    ): PolygonTransactionHistoryResponse
}