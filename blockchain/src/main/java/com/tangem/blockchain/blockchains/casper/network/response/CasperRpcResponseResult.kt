package com.tangem.blockchain.blockchains.casper.network.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Type of success response result  */
internal sealed interface CasperRpcResponseResult {

    @JsonClass(generateAdapter = true)
    data class Balance(
        @Json(name = "balance") val balance: String,
    ) : CasperRpcResponseResult

    @JsonClass(generateAdapter = true)
    data class Deploy(
        @Json(name = "deploy_hash") val deployHash: String,
    ) : CasperRpcResponseResult
}