package com.tangem.blockchain.transactionhistory.blockchains.algorand.network

import retrofit2.http.GET
import retrofit2.http.Query

internal interface AlgorandIndexerApi {

    @GET("v2/transactions")
    suspend fun getTransactions(
        @Query("address") address: String,
        @Query("limit") limit: Int,
        @Query("next") next: String?,
    ): AlgorandTransactionHistoryResponse
}