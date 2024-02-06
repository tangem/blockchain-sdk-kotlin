package com.tangem.blockchain.blockchains.cardano.network.rosetta.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RosettaAddressBody(
    @Json(name = "network_identifier")
    val networkIdentifier: RosettaNetworkIdentifier,

    @Json(name = "account_identifier")
    val accountIdentifier: RosettaAccountIdentifier,
)

@JsonClass(generateAdapter = true)
data class RosettaSubmitBody(
    @Json(name = "network_identifier")
    val networkIdentifier: RosettaNetworkIdentifier,

    @Json(name = "signed_transaction")
    val signedTransaction: String,
)

// @JsonClass(generateAdapter = true)
// data class RosettaPreprocessBody(
//        @Json(name = "network_identifier")
//        val networkIdentifier: RosettaNetworkIdentifier,
//
//        val operations: List<RosettaOperation>,
//
//        val metadata: RosettaRelativeTtlMetadata
// )
//
// @JsonClass(generateAdapter = true)
// data class RosettaMetadataBody(
//        @Json(name = "network_identifier")
//        val networkIdentifier: RosettaNetworkIdentifier,
//
//        val options: RosettaOptions
// )
//
// @JsonClass(generateAdapter = true)
// data class RosettaPayloadsBody(
//        @Json(name = "network_identifier")
//        val networkIdentifier: RosettaNetworkIdentifier,
//
//        val operations: List<RosettaOperation>,
//
//        val metadata: RosettaTtlMetadata
// )
//
// @JsonClass(generateAdapter = true)
// data class RosettaCombineBody(
//        @Json(name = "network_identifier")
//        val networkIdentifier: RosettaNetworkIdentifier,
//
//        @Json(name = "unsigned_transaction")
//        val unsignedTransaction: String,
//
//        val signatures: List<RosettaSignature>
// )
