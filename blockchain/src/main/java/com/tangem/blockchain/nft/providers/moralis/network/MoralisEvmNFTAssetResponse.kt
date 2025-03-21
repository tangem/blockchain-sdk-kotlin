package com.tangem.blockchain.nft.providers.moralis.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class MoralisEvmNFTAssetResponse(
    @Json(name = "amount") val amount: String?,
    @Json(name = "token_id") val tokenId: String?,
    @Json(name = "token_address") val tokenAddress: String?,
    @Json(name = "contract_type") val contractType: String?,
    @Json(name = "owner_of") val ownerOf: String?,
    @Json(name = "last_metadata_sync") val lastMetadataSync: String?,
    @Json(name = "last_token_uri_sync") val lastTokenUriSync: String?,
    @Json(name = "metadata") val metadata: String?,
    @Json(name = "block_number") val blockNumber: String?,
    @Json(name = "block_number_minted") val blockNumberMinted: String?,
    @Json(name = "name") val name: String?,
    @Json(name = "symbol") val symbol: String?,
    @Json(name = "token_hash") val tokenHash: String?,
    @Json(name = "token_uri") val tokenUri: String?,
    @Json(name = "minter_address") val minterAddress: String?,
    @Json(name = "rarity_rank") val rarityRank: Double?,
    @Json(name = "rarity_percentage") val rarityPercentage: Double?,
    @Json(name = "rarity_label") val rarityLabel: String?,
    @Json(name = "verified_collection") val verifiedCollection: Boolean?,
    @Json(name = "possible_spam") val possibleSpam: Boolean?,
    @Json(name = "media") val media: MoralisEvmNFTMediaResponse?,
    @Json(name = "collection_logo") val collectionLogo: String?,
    @Json(name = "collection_banner_image") val collectionBannerImage: String?,
    @Json(name = "floor_price") val floorPrice: String?,
    @Json(name = "floor_price_usd") val floorPriceUsd: String?,
    @Json(name = "floor_price_currency") val floorPriceCurrency: String?,
    @Json(name = "normalized_metadata") val normalizedMetadata: MoralisEvmNFTNormalizedMetadataResponse?,
)

@JsonClass(generateAdapter = true)
internal data class MoralisEvmNFTNormalizedMetadataResponse(
    @Json(name = "name") val name: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "animation_url") val animationUrl: String?,
    @Json(name = "external_link") val externalLink: String?,
    @Json(name = "image") val image: String?,
    @Json(name = "attributes") val attributes: List<MoralisEvmNFTAttributeResponse>?,
)

@JsonClass(generateAdapter = true)
internal data class MoralisEvmNFTAttributeResponse(
    @Json(name = "trait_type") val traitType: String?,
    @Json(name = "value") val value: Any?,
    @Json(name = "display_type") val displayType: String?,
    @Json(name = "max_value") val maxValue: Any?,
    @Json(name = "trait_count") val traitCount: Int?,
    @Json(name = "order") val order: String?,
    @Json(name = "rarity_label") val rarityLabel: String?,
    @Json(name = "count") val count: Int?,
    @Json(name = "percentage") val percentage: Double?,
)

@JsonClass(generateAdapter = true)
internal data class MoralisEvmNFTMediaResponse(
    @Json(name = "status") val status: String?,
    @Json(name = "updatedAt") val updatedAt: String?,
    @Json(name = "mimetype") val mimeType: String?,
    @Json(name = "parent_hash") val parentHash: String?,
    @Json(name = "media_collection") val mediaCollection: MoralisEvmNFTMediaCollectionResponse?,
    @Json(name = "original_media_url") val originalMediaUrl: String?,
)

@JsonClass(generateAdapter = true)
internal data class MoralisEvmNFTMediaCollectionResponse(
    @Json(name = "low") val low: MoralisEvmNFTMediaDetailResponse?,
    @Json(name = "medium") val medium: MoralisEvmNFTMediaDetailResponse?,
    @Json(name = "high") val high: MoralisEvmNFTMediaDetailResponse?,
)

@JsonClass(generateAdapter = true)
internal data class MoralisEvmNFTMediaDetailResponse(
    @Json(name = "height") val height: Int?,
    @Json(name = "width") val width: Int?,
    @Json(name = "url") val url: String?,
)