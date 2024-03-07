package com.tangem.blockchain.network.electrum

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Electrum responses
 * Supports specs:
 *
 * Original: https://bitcoincash.network/electrum/protocol-methods.html
 * Rostrum (Nexa): https://bitcoinunlimited.gitlab.io/rostrum/
 * Radiant: https://electrumx.readthedocs.io/en/latest/
 */
internal object ElectrumResponse {

    data class ServerInfo(
        val applicationName: String,
        val versionNumber: String,
    )

    @JsonClass(generateAdapter = true)
    data class BlockTip(
        @Json(name = "height") val height: Long,
        @Json(name = "hex") val hex: String,
    )

    @JsonClass(generateAdapter = true)
    data class Balance(
        @Json(name = "confirmed") val confirmed: Double,
        @Json(name = "unconfirmed") val unconfirmed: Double,
    )

    @JsonClass(generateAdapter = true)
    data class TxHistoryEntry(
        @Json(name = "fee") val fee: Double?,
        @Json(name = "height") val height: Long,
        @Json(name = "tx_hash") val txHash: String,
    )

    @JvmInline
    value class TxHex(
        val hash: String,
    )

    @JsonClass(generateAdapter = true)
    data class Transaction(
        @Json(name = "blockhash") val blockHash: String,
        @Json(name = "blocktime") val blockTime: Long,
        @Json(name = "confirmations") val confirmations: Int,
        @Json(name = "hash") val hash: String,
        @Json(name = "hex") val hex: String, // The serialized, hex-encoded data for 'txid'
        @Json(name = "locktime") val lockTime: Long,
        @Json(name = "size") val size: Long,
        @Json(name = "txid") val txid: String, // the transaction id (same as provided)
        @Json(name = "version") val version: Int,
        @Json(name = "txidem") val txidem: String?, // Nexa specific
        @Json(name = "fee") val fee: Double,
        @Json(name = "fee_satoshi") val feeSatoshi: Long,
        @Json(name = "time") val time: Long,
        @Json(name = "vin") val vin: List<Vin> = emptyList(),
        @Json(name = "vout") val vout: List<Vout> = emptyList(),
        @Json(name = "value") val value: String,
    ) {

        @JsonClass(generateAdapter = true)
        data class Vin(
            @Json(name = "value") val value: Double,
            @Json(name = "value_coins") val valueCoins: Double?, // rostrum specific (ex. Nexa)
            @Json(name = "value_satoshi") val valueSatoshi: Double?, // rostrum specific (ex. Nexa)
            @Json(name = "addresses") val addresses: List<String> = emptyList(),
        )

        @JsonClass(generateAdapter = true)
        data class Vout(
            @Json(name = "value") val value: Double,
            @Json(name = "value_coins") val valueCoins: Double?, // rostrum specific (ex. Nexa)
            @Json(name = "value_satoshi") val valueSatoshi: Double?, // rostrum specific (ex. Nexa)
            @Json(name = "scriptPubKey") val scriptPublicKey: ScriptPublicKey,
            @Json(name = "txid") val txid: String?, // bitcoincash specific
        )

        @JsonClass(generateAdapter = true)
        data class ScriptPublicKey(
            @Json(name = "addresses") val addresses: List<String> = emptyList(),
            @Json(name = "hex") val hex: String,
        )
    }
}
