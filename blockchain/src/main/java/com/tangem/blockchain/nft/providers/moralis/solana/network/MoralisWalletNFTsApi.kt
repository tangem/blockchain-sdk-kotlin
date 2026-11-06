package com.tangem.blockchain.nft.providers.moralis.solana.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Moralis wallet NFTs API (early access); the only one that returns Metaplex Core assets. */
internal interface MoralisWalletNFTsApi {

    @GET("v1/wallets/{address}/nfts")
    suspend fun getWalletNFTs(
        @Path("address") address: String,
        @Query("tokenStandards") tokenStandards: String,
        @Query("cursor") cursor: String?,
        @Query("chains") chains: String = "solana",
        @Query("limit") limit: Int = 100,
        @Query("excludeSpam") excludeSpam: Boolean = true,
        @Query("includeMetadata") includeMetadata: Boolean = true,
        @Query("includeMedia") includeMedia: Boolean = true,
    ): MoralisWalletNFTsResponse
}