package com.tangem.blockchain.blockchains.cardano.network.rosetta.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class RosettaSubmitResponse(
    @Json(name = "transaction_identifier") val transactionIdentifier: RosettaTransactionIdentifier,
) {

    @JsonClass(generateAdapter = true)
    data class RosettaTransactionIdentifier(
        @Json(name = "hash") val hash: String?,
    )
}