package com.tangem.blockchain.blockchains.filecoin.network.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Filecoin RPC response */
internal sealed interface FilecoinRpcResponse {

    /** Success response with [result] */
    data class Success(val result: Any) : FilecoinRpcResponse

    /** Failure response with error [message] */
    @JsonClass(generateAdapter = true)
    data class Failure(
        @Json(name = "message") val message: String,
    ) : FilecoinRpcResponse
}
