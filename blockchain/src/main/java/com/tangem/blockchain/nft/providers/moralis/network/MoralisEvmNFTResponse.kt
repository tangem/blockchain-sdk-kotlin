package com.tangem.blockchain.nft.providers.moralis.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class MoralisEvmNFTResponse<T>(
    @Json(name = "status") val status: String?,
    @Json(name = "message") val message: String?,
    @Json(name = "page") val page: Int?,
    @Json(name = "cursor") val cursor: String?,
    @Json(name = "page_size") val pageSize: Int?,
    @Json(name = "result") val result: List<T>?,
)