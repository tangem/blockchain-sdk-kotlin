package com.tangem.blockchain.blockchains.filecoin.network.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Filecoin RPC body
 *
[REDACTED_AUTHOR]
 */
// TODO: [REDACTED_JIRA]
@JsonClass(generateAdapter = true)
internal data class FilecoinRpcBody(
    @Json(name = "id") val id: Int,
    @Json(name = "jsonrpc") val jsonrpc: String,
    @Json(name = "method") val method: Method,
    @Json(name = "params") val params: List<Any?>,
) {

    enum class Method {

        @Json(name = "Filecoin.StateGetActor")
        GetActorInfo,

        @Json(name = "Filecoin.GasEstimateMessageGas")
        GetMessageGas,

        @Json(name = "Filecoin.MpoolPush")
        SubmitTransaction,
    }
}