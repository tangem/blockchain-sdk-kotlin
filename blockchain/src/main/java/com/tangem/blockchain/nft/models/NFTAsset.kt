package com.tangem.blockchain.nft.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class NFTAsset(
    @Json(name = "identifier") val identifier: Identifier,
    @Json(name = "collectionIdentifier") val collectionIdentifier: NFTCollection.Identifier,
    @Json(name = "blockchainId") val blockchainId: String,
    @Json(name = "contractType") val contractType: String,
    @Json(name = "owner") val owner: String?,
    @Json(name = "name") val name: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "salePrice") val salePrice: SalePrice?,
    @Json(name = "rarity") val rarity: Rarity?,
    @Json(name = "media") val media: Media?,
    @Json(name = "traits") val traits: List<Trait>,
) {
    sealed class Identifier {
        @JsonClass(generateAdapter = true)
        data class EVM(
            @Json(name = "tokenId") val tokenId: String,
            @Json(name = "tokenAddress") val tokenAddress: String,
        ) : Identifier()

        @JsonClass(generateAdapter = true)
        data class TON(
            @Json(name = "tokenAddress") val tokenAddress: String,
        ) : Identifier()

        @JsonClass(generateAdapter = true)
        data class Solana(
            @Json(name = "tokenAddress") val tokenAddress: String,
            @Json(name = "cnft") val cnft: Boolean,
        ) : Identifier()

        data object Unknown : Identifier()
    }

    @JsonClass(generateAdapter = true)
    data class Media(
        @Json(name = "mimetype") val mimetype: String?,
        @Json(name = "url") val url: String,
    )

    @JsonClass(generateAdapter = true)
    data class Rarity(
        @Json(name = "rank") val rank: String,
        @Json(name = "label") val label: String,
    )

    @JsonClass(generateAdapter = true)
    data class Trait(
        @Json(name = "name") val name: String,
        @Json(name = "value") val value: String,
    )

    @JsonClass(generateAdapter = true)
    data class SalePrice(
        @Json(name = "value") val value: BigDecimal,
        @Json(name = "symbol") val symbol: String,
    )
}