package com.tangem.blockchain.nft.providers.nftscan.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface NFTScanTonApi {

    @GET("api/ton/account/own/all/{account_address}")
    suspend fun getNFTCollections(
        @Path("account_address") accountAddress: String,
        @Query("show_attribute") showAttribute: Boolean = true,
    ): NFTScanTonNFTCollectionsResponse

    @GET("api/ton/assets/{token_address}")
    suspend fun getNFTAsset(
        @Path("token_address") tokenAddress: String,
        @Query("show_attribute") showAttribute: Boolean = true,
    )
}