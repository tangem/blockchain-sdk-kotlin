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
        @Query("startblock") startBlock: Long = 0,
        @Query("endblock") endBlock: Long = DEFAULT_END_BLOCK,
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
        @Query("startblock") startBlock: Long = 0,
        @Query("endblock") endBlock: Long = DEFAULT_END_BLOCK,
        @Query("sort") sort: String = "desc",
    ): PolygonTransactionHistoryResponse

    @Suppress("LongParameterList")
    @GET("api")
    suspend fun getInternalTransactionHistory(
        @Query("chainid") chainId: Int,
        @Query("address") address: String,
        @Query("page") page: Int,
        @Query("offset") offset: Int,
        @Query("apikey") apiKey: String,
        @Query("endblock") endBlock: Long = DEFAULT_END_BLOCK,
        @Query("action") action: String = "txlistinternal",
        @Query("module") module: String = "account",
        @Query("startblock") startBlock: Long = 0,
        @Query("sort") sort: String = "desc",
    ): EtherscanInternalTransactionsResponse

    companion object {
        const val DEFAULT_END_BLOCK: Long = Int.MAX_VALUE.toLong()
    }
}