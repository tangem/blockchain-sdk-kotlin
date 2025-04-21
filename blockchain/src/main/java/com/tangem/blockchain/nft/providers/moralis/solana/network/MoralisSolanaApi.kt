package com.tangem.blockchain.nft.providers.moralis.solana.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface MoralisSolanaApi {

    @GET("account/mainnet/{address}/nft")
    suspend fun getNFTAssets(
        @Path("address") address: String,
        @Query("nftMetadata") nftMetadata: Boolean = true,
    ): List<MoralisSolanaNFTAssetResponse>
}