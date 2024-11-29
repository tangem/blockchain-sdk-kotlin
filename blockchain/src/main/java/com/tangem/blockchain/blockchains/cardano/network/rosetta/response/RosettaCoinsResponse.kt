package com.tangem.blockchain.blockchains.cardano.network.rosetta.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class RosettaCoinsResponse(
    @Json(name = "coins") val coins: List<Coin>,
) {

    @JsonClass(generateAdapter = true)
    data class Coin(
        @Json(name = "coin_identifier") val coinIdentifier: CoinIdentifier,
        @Json(name = "amount") val amount: Amount,
        @Json(name = "metadata") val metadata: Map<String, MetadataValue>?,
    ) {

        @JsonClass(generateAdapter = true)
        data class CoinIdentifier(
            @Json(name = "identifier") val identifier: String,
        )

        @JsonClass(generateAdapter = true)
        data class MetadataValue(
            @Json(name = "policyId") val policyId: String,
            @Json(name = "tokens") val tokens: List<Amount>,
        )

        @JsonClass(generateAdapter = true)
        data class Amount(
            @Json(name = "value") val value: String,
            @Json(name = "currency") val currency: Currency,
        ) {

            @JsonClass(generateAdapter = true)
            data class Currency(
                @Json(name = "symbol")
                val symbol: String?,
                @Json(name = "decimals")
                val decimals: Int?,
                @Json(name = "metadata")
                val metadata: Metadata?,
            ) {

                @JsonClass(generateAdapter = true)
                data class Metadata(
                    @Json(name = "policyId") val policyId: String?,
                )
            }
        }
    }
}