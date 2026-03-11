package com.tangem.blockchain.tokenbalance.providers.moralis.evm.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface MoralisEvmTokenBalanceApi {

    @GET("api/v2.2/wallets/{address}/tokens")
    suspend fun getTokenBalances(
        @Path("address") address: String,
        @Query("chain") chain: String,
        @Query("exclude_spam") excludeSpam: Boolean = true,
        @Query("exclude_native") excludeNative: Boolean = false,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = PAGINATION_LIMIT,
    ): MoralisEvmTokenBalanceResponse

    private companion object {
        const val PAGINATION_LIMIT = 100
    }
}