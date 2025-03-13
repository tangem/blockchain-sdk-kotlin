package com.tangem.blockchain.nft.providers.nftscan.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class NFTScanTonPaginationResponse<T>(
    @Json(name = "total") val total: Int,
    @Json(name = "next") val next: String?,
    @Json(name = "content") val content: T?,
)