package com.tangem.blockchain.blockchains.sui.network.rpc

import com.squareup.moshi.Json
import java.util.UUID

internal data class SuiJsonRpcRequestBody(
    @Json(name = "id") val id: String = UUID.randomUUID().toString(),
    @Json(name = "jsonrpc") val jsonRpc: String = "2.0",
    @Json(name = "method") val method: String,
    @Json(name = "params") val params: List<Any>,
)
