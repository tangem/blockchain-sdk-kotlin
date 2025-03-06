package com.tangem.blockchain.nft.providers.moralis.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class MoralisEvmNFTGetAssetsRequest(
    @Json(name = "tokens") val tokens: List<MoralisEvmNFTGetAssetsTokenRequest>,
    @Json(name = "normalizeMetadata") val normalizeMetadata: Boolean = true,
    @Json(name = "media_items") val mediaItems: Boolean = true,
)

@JsonClass(generateAdapter = true)
internal data class MoralisEvmNFTGetAssetsTokenRequest(
    @Json(name = "token_address") val tokenAddress: String,
    @Json(name = "token_id") val tokenId: Int,
)