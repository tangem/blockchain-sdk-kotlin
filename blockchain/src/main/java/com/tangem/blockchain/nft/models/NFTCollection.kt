package com.tangem.blockchain.nft.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.blockchain.common.Blockchain
import kotlinx.serialization.Serializable

@JsonClass(generateAdapter = true)
data class NFTCollection(
    @Json(name = "identifier") val identifier: Identifier,
    @Json(name = "blockchain") val blockchain: Blockchain,
    @Json(name = "name") val name: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "logoUrl") val logoUrl: String?,
    @Json(name = "count") val count: Int,
    @Json(name = "assets") val assets: List<NFTAsset> = emptyList(),
) {
    @Serializable
    sealed class Identifier {
        @Serializable
        @JsonClass(generateAdapter = true)
        data class EVM(
            @Json(name = "tokenAddress") val tokenAddress: String,
        ) : Identifier()

        @Serializable
        @JsonClass(generateAdapter = true)
        data class TON(
            @Json(name = "contractAddress") val contractAddress: String?,
        ) : Identifier()
    }
}