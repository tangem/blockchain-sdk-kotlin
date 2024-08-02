package com.tangem.blockchain.blockchains.filecoin.network.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Type of success response result  */
internal sealed interface FilecoinRpcResponseResult {

    @JsonClass(generateAdapter = true)
    data class GetActorInfo(
        @Json(name = "Balance") val balance: String,
        @Json(name = "Nonce") val nonce: Long,
    ) : FilecoinRpcResponseResult

    @JsonClass(generateAdapter = true)
    data class GetMessageGas(
        @Json(name = "GasFeeCap") val gasUnitPrice: String,
        @Json(name = "GasLimit") val gasLimit: Long,
        @Json(name = "GasPremium") val gasPremium: String,
    ) : FilecoinRpcResponseResult

    @JsonClass(generateAdapter = true)
    data class SubmitTransaction(
        @Json(name = "/") val hash: String,
    ) : FilecoinRpcResponseResult
}