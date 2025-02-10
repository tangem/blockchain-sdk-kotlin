package com.tangem.blockchain.nft.providers.nftscan.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class NFTScanTonNFTCollectionResponse(
    @Json(name = "contract_name") val contractName: String?,
    @Json(name = "contract_address") val contractAddress: String?,
    @Json(name = "logo_url") val logoUrl: String?,
    @Json(name = "owns_total") val ownsTotal: Int?,
    @Json(name = "items_total") val itemsTotal: Int?,
    @Json(name = "description") val description: String?,
    @Json(name = "assets") val assets: List<NFTScanTonNFTAssetResponse>?,
)