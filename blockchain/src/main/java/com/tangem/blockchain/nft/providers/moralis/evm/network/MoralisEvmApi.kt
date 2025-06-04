package com.tangem.blockchain.nft.providers.moralis.evm.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

internal interface MoralisEvmApi {

    @GET("api/v2.2/{address}/nft/collections")
    suspend fun getNFTCollections(
        @Path("address") address: String,
        @Query("chain") chain: String,
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int,
        @Query("exclude_spam") excludeSpam: Boolean = true,
        @Query("token_counts") tokenCounts: Boolean = true,
    ): MoralisEvmNFTResponse<MoralisEvmNFTCollectionResponse>

    @GET("api/v2.2/{address}/nft")
    suspend fun getNFTAssets(
        @Path("address") address: String,
        @Query("token_addresses[]") tokenAddresses: List<String>,
        @Query("chain") chain: String,
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int,
        @Query("format") format: String = "decimal",
        @Query("exclude_spam") excludeSpam: Boolean = true,
        @Query("normalizeMetadata") normalizeMetadata: Boolean = true,
        @Query("media_items") mediaItems: Boolean = true,
    ): MoralisEvmNFTResponse<MoralisEvmNFTAssetResponse>

    @POST("api/v2.2/nft/getMultipleNFTs")
    suspend fun getNFTAssets(@Body request: MoralisEvmNFTGetAssetsRequest): List<MoralisEvmNFTAssetResponse>

    @GET("api/v2.2/nft/{token_address}/{token_id}/price")
    suspend fun getNFTPrice(
        @Path("token_address") tokenAddress: String,
        @Path("token_id") tokenId: String,
        @Query("chain") chain: String,
        @Query("days") days: Int = 7,
    ): MoralisEvmNFTPricesResponse
}