package com.tangem.blockchain.network.blockcypher.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SendTransactionResponse(
    @Json(name = "tx") val transactionData: TransactionData,
) {

    @JsonClass(generateAdapter = true)
    data class TransactionData(
        @Json(name = "hash") val hash: String,
    )
}
