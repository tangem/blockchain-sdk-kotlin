package com.tangem.blockchain.nft.providers.moralis.evm.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class MoralisEvmNFTCollectionResponse(
    @Json(name = "token_address") val tokenAddress: String?,
    @Json(name = "possible_spam") val possibleSpam: Boolean?,
    @Json(name = "contract_type") val contractType: String?,
    @Json(name = "name") val name: String?,
    @Json(name = "symbol") val symbol: String?,
    @Json(name = "verified_collection") val verifiedCollection: Boolean?,
    @Json(name = "collection_logo") val collectionLogo: String?,
    @Json(name = "collection_banner_image") val collectionBannerImage: String?,
    @Json(name = "floor_price") val floorPrice: String?,
    @Json(name = "floor_price_usd") val floorPriceUsd: String?,
    @Json(name = "floor_price_currency") val floorPriceCurrency: String?,
    @Json(name = "count") val count: Int?,
)