package com.tangem.blockchain.network.electrum.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * Electrum responses
 * Supports specs:
 *
 * Original: https://bitcoincash.network/electrum/protocol-methods.html
 * Rostrum (Nexa): https://bitcoinunlimited.gitlab.io/rostrum/
 * Radiant: https://electrumx.readthedocs.io/en/latest/
 * Fact0rn: https://electrumx-spesmilo.readthedocs.io/en/latest//
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
        @Json(name = "confirmed") val satoshiConfirmed: Long,
        @Json(name = "unconfirmed") val satoshiUnconfirmed: Long,
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

    @JvmInline
    value class EstimateFee(
        // null if the daemon does not have enough information to make an estimate
        val feeInCoinsPer1000Bytes: BigDecimal?,
    )

    @JsonClass(generateAdapter = true)
    data class UnspentUTXORecord(
        // The integer height of the block the transaction was confirmed in. 0 if the transaction is in the mempool.
        @Json(name = "height") val height: Long,
        // The zero-based index of the output in the transaction’s list of outputs.
        @Json(name = "tx_pos") val txPos: Long,
        // The output’s transaction hash as a hexadecimal string.
        @Json(name = "tx_hash") val txHash: String,
        // The output’s value in minimum coin units (satoshis).
        @Json(name = "value") val valueSatoshi: Long,
        // Hash of utxo (hash of transaction idem + output index)
        @Json(name = "outpoint_hash") val outpointHash: String?, // TODO is Nexa epecific?
    )

    @JsonClass(generateAdapter = true)
    data class Transaction(
        @Json(name = "blockhash") val blockHash: String?,
        @Json(name = "blocktime") val blockTime: Long?,
        @Json(name = "confirmations") val confirmations: Int?,
        @Json(name = "fee") val fee: Double?,
        @Json(name = "fee_satoshi") val feeSatoshi: Long?,
        @Json(name = "hash") val hash: String,
        @Json(name = "hex") val hex: String, // The serialized, hex-encoded data for 'txid'
        @Json(name = "locktime") val lockTime: Long,
        @Json(name = "size") val size: Long,
        @Json(name = "time") val time: Long?,
        @Json(name = "txid") val txid: String, // the transaction id (same as provided)
        @Json(name = "txidem") val txidem: String?, // Nexa specific
        @Json(name = "version") val version: Int,
        @Json(name = "vin") val vin: List<Vin>? = emptyList(),
        @Json(name = "vout") val vout: List<Vout>? = emptyList(),
    ) {

        @JsonClass(generateAdapter = true)
        data class Vin(
            @Json(name = "value") val value: Double?,
            @Json(name = "value_coins") val valueCoins: Double?, // rostrum specific (ex. Nexa)
            @Json(name = "value_satoshi") val valueSatoshi: Double?, // rostrum specific (ex. Nexa)
            @Json(name = "addresses") val addresses: List<String>? = emptyList(),
            @Json(name = "txinwitness") val txinwitness: List<String>? = emptyList(),
        )

        @JsonClass(generateAdapter = true)
        data class Vout(
            @Json(name = "value") val value: Double,
            @Json(name = "value_coins") val valueCoins: Double?, // rostrum specific (ex. Nexa)
            @Json(name = "value_satoshi") val valueSatoshi: Double?, // rostrum specific (ex. Nexa)
            @Json(name = "scriptPubKey") val scriptPublicKey: ScriptPublicKey?,
            @Json(name = "txid") val txid: String?, // bitcoincash specific
        )

        @JsonClass(generateAdapter = true)
        data class ScriptPublicKey(
            @Json(name = "addresses") val addresses: List<String> = emptyList(),
            @Json(name = "address") val address: String?,
            @Json(name = "hex") val hex: String,
            @Json(name = "scriptHash") val scriptHash: String?,
        )
    }
}