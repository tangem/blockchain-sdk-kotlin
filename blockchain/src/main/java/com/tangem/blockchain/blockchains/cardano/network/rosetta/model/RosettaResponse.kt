package com.tangem.blockchain.blockchains.cardano.network.rosetta.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RosettaBalanceResponse(
    val balances: List<RosettaAmount>? = null,
)

@JsonClass(generateAdapter = true)
data class RosettaCoinsResponse(
    val coins: List<RosettaCoin>? = null,
)

@JsonClass(generateAdapter = true)
data class RosettaSubmitResponse(
    @Json(name = "transaction_identifier")
    val transactionIdentifier: RosettaTransactionIdentifier? = null,
)

// @JsonClass(generateAdapter = true)
// data class RosettaPreprocessResponse(
//        val options: RosettaOptions? = null
// )
//
// @JsonClass(generateAdapter = true)
// data class RosettaMetadataResponse(
//        val metadata: RosettaTtlMetadata? = null,
//
//        @Json(name = "suggested_fee")
//        val suggestedFee: List<RosettaAmount>? = null
// )
//
// @JsonClass(generateAdapter = true)
// data class RosettaPayloadsResponse(
//        @Json(name = "unsigned_transaction")
//        val unsignedTransaction: String? = null,
//
//        val payloads: List<RosettaSigningPayload>? = null
// )
//
// @JsonClass(generateAdapter = true)
// data class RosettaCombineResponse(
//        @Json(name = "signed_transaction")
//        val signedTransaction: String? = null
// )
