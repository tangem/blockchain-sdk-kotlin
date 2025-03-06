package com.tangem.blockchain.nft.providers.nftscan.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class NFTScanTonNFTCollectionsResponse(
    @Json(name = "code") val code: Int,
    @Json(name = "msg") val message: String?,
    @Json(name = "data") val data: List<NFTScanTonNFTCollectionResponse>?,
)