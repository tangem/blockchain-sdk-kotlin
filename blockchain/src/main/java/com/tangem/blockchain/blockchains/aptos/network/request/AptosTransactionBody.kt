package com.tangem.blockchain.blockchains.aptos.network.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class AptosTransactionBody(
    @Json(name = "expiration_timestamp_secs") val expirationTimestamp: String,
    @Json(name = "gas_unit_price") val gasUnitPrice: String,
    @Json(name = "max_gas_amount") val maxGasAmount: String,
    @Json(name = "payload") val payload: Payload,
    @Json(name = "sender") val sender: String,
    @Json(name = "sequence_number") val sequenceNumber: String,
    @Json(name = "signature") val signature: Signature,
) {

    @JsonClass(generateAdapter = true)
    data class Payload(
        @Json(name = "type") val type: String,
        @Json(name = "function") val function: String,
        @Json(name = "type_arguments") val argumentTypes: List<String>,
        @Json(name = "arguments") val arguments: List<String>,
    )

    @JsonClass(generateAdapter = true)
    data class Signature(
        @Json(name = "type") val type: String,
        @Json(name = "public_key") val publicKey: String,
        @Json(name = "signature") val signature: String,
    )
}
