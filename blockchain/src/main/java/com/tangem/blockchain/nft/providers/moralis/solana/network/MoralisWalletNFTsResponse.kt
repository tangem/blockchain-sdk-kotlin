package com.tangem.blockchain.nft.providers.moralis.solana.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * All fields are nullable, so an item with missing or null fields still parses. A field of the wrong type still
 * fails the whole page.
 */
@JsonClass(generateAdapter = true)
internal data class MoralisWalletNFTsResponse(
    @Json(name = "meta") val meta: Meta?,
    @Json(name = "cursor") val cursor: String?,
    @Json(name = "result") val result: List<Item>?,
) {

    @JsonClass(generateAdapter = true)
    data class Meta(@Json(name = "failedChains") val failedChains: List<Any>?)

    @JsonClass(generateAdapter = true)
    data class Item(
        @Json(name = "tokenAddress") val tokenAddress: String?,
        @Json(name = "tokenStandard") val tokenStandard: String?,
        @Json(name = "name") val name: String?,
        @Json(name = "metadata") val metadata: Metadata?,
        @Json(name = "media") val media: Media?,
        @Json(name = "collection") val collection: Collection?,
    )

    @JsonClass(generateAdapter = true)
    data class Metadata(
        @Json(name = "name") val name: String?,
        @Json(name = "description") val description: String?,
        @Json(name = "image") val image: String?,
        @Json(name = "animationUrl") val animationUrl: String?,
        @Json(name = "attributes") val attributes: List<Attribute>?,
    )

    @JsonClass(generateAdapter = true)
    data class Attribute(
        @Json(name = "traitType") val traitType: String?,
        @Json(name = "value") val value: Any?,
    )

    @JsonClass(generateAdapter = true)
    data class Media(
        @Json(name = "mimetype") val mimetype: String?,
        @Json(name = "originalMediaUrl") val originalMediaUrl: String?,
        @Json(name = "mediaCollection") val mediaCollection: MediaCollection?,
    )

    @JsonClass(generateAdapter = true)
    data class MediaCollection(
        @Json(name = "low") val low: MediaSize?,
        @Json(name = "medium") val medium: MediaSize?,
        @Json(name = "high") val high: MediaSize?,
    )

    @JsonClass(generateAdapter = true)
    data class MediaSize(@Json(name = "url") val url: String?)

    @JsonClass(generateAdapter = true)
    data class Collection(
        @Json(name = "address") val address: String?,
        @Json(name = "name") val name: String?,
    )
}