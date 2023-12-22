package com.tangem.blockchain.blockchains.cardano.network.rosetta.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RosettaNetworkIdentifier(
    val blockchain: String? = null,
    val network: String? = null,
)

@JsonClass(generateAdapter = true)
data class RosettaAccountIdentifier(
    val address: String? = null,
)

@JsonClass(generateAdapter = true)
data class RosettaAmount(
    val value: Long? = null,
    val currency: RosettaCurrency? = null,
)

@JsonClass(generateAdapter = true)
data class RosettaCurrency(
    val symbol: String? = null,
    val decimals: Int? = null,
)

@JsonClass(generateAdapter = true)
data class RosettaCoin(
    @Json(name = "coin_identifier")
    val coinIdentifier: RosettaCoinIdentifier? = null,
    val amount: RosettaAmount? = null,
    @Json(name = "metadata")
    val metadata: RosettaMetadata? = null,
)

@JsonClass(generateAdapter = true)
data class RosettaMetadata(
    val metadata: Map<String, List<Any>>? = null,
)

@JsonClass(generateAdapter = true)
data class RosettaCoinIdentifier(
    val identifier: String? = null,
)

@JsonClass(generateAdapter = true)
data class RosettaTransactionIdentifier(
    val hash: String? = null,
)

// @JsonClass(generateAdapter = true)
// data class RosettaOperation(
//        @Json(name = "operation_identifier")
//        val operationIdentifier: RosettaOperationIdentifier? = null,
//
//        val type: String? = null,
//
//        val account: RosettaAccountIdentifier? = null,
//
//        val amount: RosettaAmount? = null,
//
//        @Json(name = "coin_change")
//        val coinChange: RosettaCoinChange? = null,
//
//        val status: String? = null
// )
//
// @JsonClass(generateAdapter = true)
// data class RosettaOperationIdentifier(
//        val index: Int? = null
// )
//
// @JsonClass(generateAdapter = true)
// data class RosettaCoinChange(
//        @Json(name = "coin_action")
//        val coinAction: String? = null,
//
//        @Json(name = "coin_identifier")
//        val coinIdentifier: RosettaCoinIdentifier? = null
// )
//
// @JsonClass(generateAdapter = true)
// data class RosettaRelativeTtlMetadata(
//        @Json(name = "relative_ttl")
//        val relativeTtl: Int? = null
// )
//
// @JsonClass(generateAdapter = true)
// data class RosettaTtlMetadata(
//        val ttl: String? = null
// )
//
// @JsonClass(generateAdapter = true)
// data class RosettaOptions(
//        @Json(name = "relative_ttl")
//        val relativeTtl: Int? = null,
//
//        @Json(name = "transaction_size")
//        val transactionSize: Int? = null
// )
//
// @JsonClass(generateAdapter = true)
// data class RosettaSigningPayload(
//        val address: String? = null,
//
//        @Json(name = "hex_bytes")
//        val hexBytes: String? = null,
//
//        @Json(name = "signature_type")
//        val signatureType: String? = null
// )
//
// @JsonClass(generateAdapter = true)
// data class RosettaSignature(
//        @Json(name = "signing_payload")
//        val signingPayload: RosettaSigningPayload? = null,
//
//        @Json(name = "public_key")
//        val publicKey: RosettaPublicKey? = null,
//
//        @Json(name = "signature_type")
//        val signatureType: String? = null,
//
//        @Json(name = "hex_bytes")
//        val hexBytes: String? = null
// )
//
// @JsonClass(generateAdapter = true)
// data class RosettaPublicKey(
//        @Json(name = "hex_bytes")
//        val hexBytes: String? = null,
//
//        @Json(name = "curve_type")
//        val curve_type: String? = null
// )
