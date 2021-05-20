package com.tangem.blockchain.network.blockcypher

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BlockcypherAddress(
        @Json(name = "address")
        val address: String? = null,

        @Json(name = "final_balance")
        val balance: Long? = null,

        @Json(name = "txrefs")
        val txrefs: List<BlockcypherTxref>? = null,

        @Json(name = "unconfirmed_txrefs")
        val unconfirmedTxrefs: List<BlockcypherTxref>? = null,

        @Json(name = "unconfirmed_balance")
        val unconfirmedBalance: Long? = null
)

@JsonClass(generateAdapter = true)
data class BlockcypherTxref(
        @Json(name = "tx_hash")
        val hash: String? = null,

        @Json(name = "tx_output_n")
        val outputIndex: Int? = null,

        val value: Long? = null,

        @Json(name = "confirmations")
        val confirmations: Long? = null,

        @Json(name = "script")
        val outputScript: String? = null,

        @Json(name = "spent")
        var spent: Boolean? = null,

        @Json(name = "received")
        var received: String? = null
)

@JsonClass(generateAdapter = true)
data class BlockcypherRawTx(
        @Json(name = "hex")
        val hex: String? = null
)

@JsonClass(generateAdapter = true)
data class BlockcypherTransaction(
        @Json(name = "block_height")
        val block: Int? = null,

        val hash: String? = null,

        val received: String? = null,

        val confirmed: String? = null,

        val inputs: List<BlockcypherInput>? = null,

        val outputs: List<BlockcypherOutput>? = null
)

@JsonClass(generateAdapter = true)
data class BlockcypherInput(
        @Json(name = "prev_hash")
        val transactionHash: String? = null,

        @Json(name = "output_index")
        val index: Int? = null,

        val script: String? = null,

        @Json(name = "output_value")
        val value: Long? = null,

        val sequence: Long? = null,

        val addresses: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class BlockcypherOutput(
        val value: Long? = null,

        val addresses: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class BlockcypherFee(
        @Json(name = "low_fee_per_kb")
        val minFeePerKb: Long? = null,

        @Json(name = "medium_fee_per_kb")
        val normalFeePerKb: Long? = null,

        @Json(name = "high_fee_per_kb")
        val priorityFeePerKb: Long? = null
)