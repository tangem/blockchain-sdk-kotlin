package com.tangem.blockchain.nft.providers.moralis.solana.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class MoralisSolanaNFTAssetResponse(
    @Json(name = "associatedTokenAddress") val associatedTokenAddress: String?,
    @Json(name = "mint") val mint: String?,
    @Json(name = "name") val name: String?,
    @Json(name = "symbol") val symbol: String?,
    @Json(name = "decimals") val decimals: Int?,
    @Json(name = "amount") val amount: String?,
    @Json(name = "amountRaw") val amountRaw: String?,
    @Json(name = "totalSupply") val totalSupply: String?,
    @Json(name = "attributes") val attributes: List<Attribute>?,
    @Json(name = "contract") val contract: Contract?,
    @Json(name = "collection") val collection: Collection?,
    @Json(name = "firstCreated") val firstCreated: FirstCreated?,
    @Json(name = "creators") val creators: List<Creator>?,
    @Json(name = "properties") val properties: Properties?,
) {
    @JsonClass(generateAdapter = true)
    data class Attribute(
        @Json(name = "traitType") val traitType: String?,
        @Json(name = "value") val value: Any?,
    )

    @JsonClass(generateAdapter = true)
    data class Contract(
        @Json(name = "type") val type: String?,
        @Json(name = "name") val name: String?,
        @Json(name = "symbol") val symbol: String?,
    )

    @JsonClass(generateAdapter = true)
    data class Collection(
        @Json(name = "collectionAddress") val collectionAddress: String?,
        @Json(name = "name") val name: String?,
        @Json(name = "description") val description: String?,
        @Json(name = "imageOriginalUrl") val imageOriginalUrl: String?,
        @Json(name = "externalUrl") val externalUrl: String?,
        @Json(name = "metaplexMint") val metaplexMint: String?,
        @Json(name = "sellerFeeBasisPoints") val sellerFeeBasisPoints: Int?,
    )

    @JsonClass(generateAdapter = true)
    data class FirstCreated(
        @Json(name = "mintTimestamp") val mintTimestamp: Long?,
        @Json(name = "mintBlockNumber") val mintBlockNumber: Long?,
        @Json(name = "mintTransaction") val mintTransaction: String?,
    )

    @JsonClass(generateAdapter = true)
    data class Creator(
        @Json(name = "address") val address: String?,
        @Json(name = "share") val share: Int?,
        @Json(name = "verified") val verified: Boolean?,
    )

    @JsonClass(generateAdapter = true)
    data class Properties(
        @Json(name = "files") val files: List<MoralisSolanaNFTFileResponse>?,
        @Json(name = "category") val category: String?,
        @Json(name = "creators") val creators: List<SimpleCreator>?,
    )

    @JsonClass(generateAdapter = true)
    data class MoralisSolanaNFTFileResponse(
        @Json(name = "uri") val uri: String?,
        @Json(name = "type") val type: String?,
    )

    @JsonClass(generateAdapter = true)
    data class SimpleCreator(
        @Json(name = "address") val address: String?,
        @Json(name = "share") val share: Int?,
    )
}