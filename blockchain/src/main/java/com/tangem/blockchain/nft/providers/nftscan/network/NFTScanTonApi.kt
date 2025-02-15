package com.tangem.blockchain.nft.providers.nftscan.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface NFTScanTonApi {

    @GET("api/ton/account/own/all/{account_address}")
    suspend fun getNFTCollections(
        @Path("account_address") accountAddress: String,
        @Query("show_attribute") showAttribute: Boolean = true,
    ): NFTScanTonResponse<List<NFTScanTonCollectionResponse>?>

    @GET("api/ton/account/own/{account_address}")
    suspend fun getNFTAssets(
        @Path("account_address") accountAddress: String,
        @Query("contract_address") contractAddress: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("show_attribute") showAttribute: Boolean = true,
    ): NFTScanTonResponse<NFTScanTonPaginationResponse<List<NFTScanTonAssetResponse>?>?>

    @GET("api/ton/assets/{token_address}")
    suspend fun getNFTAsset(
        @Path("token_address") tokenAddress: String,
        @Query("show_attribute") showAttribute: Boolean = true,
    ): NFTScanTonResponse<NFTScanTonAssetResponse?>
}