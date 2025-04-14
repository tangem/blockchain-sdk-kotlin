package com.tangem.blockchain.network.blockchair.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SendTransactionResponse(
    @Json(name = "data") val transactionData: TransactionData,
) {

    @JsonClass(generateAdapter = true)
    data class TransactionData(
        @Json(name = "transaction_hash") val hash: String,
    )
}
