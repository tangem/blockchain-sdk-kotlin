package com.tangem.blockchain.blockchains.near.network.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Basic type of JSON RPC responses for the NearApi.
 *
 * @see NearApi
 * @author Anton Zhilenkov on 31.07.2023.
 */
@JsonClass(generateAdapter = true)
internal data class NearResponse<Result>(
    @Json(name = "jsonrpc") val jsonRpc: String,
    @Json(name = "id") val id: String,
    @Json(name = "result") val result: Result?,
    @Json(name = "error") val error: ErrorResult?,
)

@JsonClass(generateAdapter = true)
internal data class ErrorResult(
    @Json(name = "name") val name: String,
    @Json(name = "cause") val cause: Cause,
) {

    @JsonClass(generateAdapter = true)
    data class Cause(
        @Json(name = "info") val info: Any,
        @Json(name = "name") val name: String,
    )
}
