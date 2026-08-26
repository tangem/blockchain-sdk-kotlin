package com.tangem.blockchain.blockchains.tron.gasless

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** TronWeb-compatible signed transaction — the exact shape the gasless backend `JSON.parse`s. */
@JsonClass(generateAdapter = true)
internal data class TronWebSignedTransaction(
    @Json(name = "visible") val isVisible: Boolean = false,
    @Json(name = "txID") val txId: String,
    @Json(name = "raw_data") val rawData: TronWebRawData,
    @Json(name = "raw_data_hex") val rawDataHex: String,
    @Json(name = "signature") val signature: List<String>,
)

@JsonClass(generateAdapter = true)
internal data class TronWebRawData(
    @Json(name = "contract") val contract: List<TronWebContract>,
    @Json(name = "ref_block_bytes") val refBlockBytes: String,
    @Json(name = "ref_block_hash") val refBlockHash: String,
    @Json(name = "expiration") val expiration: Long,
    @Json(name = "fee_limit") val feeLimit: Long?,
    @Json(name = "timestamp") val timestamp: Long,
)

@JsonClass(generateAdapter = true)
internal data class TronWebContract(
    @Json(name = "parameter") val parameter: TronWebParameter,
    @Json(name = "type") val type: String,
)

@JsonClass(generateAdapter = true)
internal data class TronWebParameter(
    @Json(name = "value") val value: TronWebTriggerValue,
    @Json(name = "type_url") val typeUrl: String,
)

@JsonClass(generateAdapter = true)
internal data class TronWebTriggerValue(
    @Json(name = "data") val data: String,
    @Json(name = "owner_address") val ownerAddress: String,
    @Json(name = "contract_address") val contractAddress: String,
)