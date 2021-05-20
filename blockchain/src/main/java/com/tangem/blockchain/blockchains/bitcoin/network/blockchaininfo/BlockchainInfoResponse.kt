package com.tangem.blockchain.blockchains.bitcoin.network.blockchaininfo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BlockchainInfoAddress(
        @Json(name = "final_balance")
        val finalBalance: Long? = null,

        @Json(name = "n_tx")
        val transactionCount: Int? = null,

        @Json(name = "txs")
        val transactions: List<BlockchainInfoTransaction>? = null
)

@JsonClass(generateAdapter = true)
data class BlockchainInfoTransaction(
        val hash: String? = null,

        @Json(name = "block_height")
        val block: Long? = null,

        @Json(name = "result")
        val balanceDif: Long? = null, // in address data only

        @Json(name = "vin_sz")
        val inputCount: Int? = null,

        val time: Long? = null,

        val inputs: List<BlockchainInfoInput>? = null,

        @Json(name = "out")
        val outputs: List<BlockchainInfoOutput>? = null
)

@JsonClass(generateAdapter = true)
data class BlockchainInfoUnspents(
        @Json(name = "unspent_outputs")
        val unspentOutputs: List<BlockchainInfoUtxo>? = null
)

@JsonClass(generateAdapter = true)
data class BlockchainInfoUtxo(
        @Json(name = "tx_hash_big_endian")
        val hash: String? = null,

        @Json(name = "tx_output_n")
        val outputIndex: Int? = null,

        @Json(name = "value")
        val amount: Long? = null,

        @Json(name = "script")
        val outputScript: String? = null
)

@JsonClass(generateAdapter = true)
data class BlockchainInfoFees(
        @Json(name = "regular")
        val regularFeePerByte: Int? = null,

        @Json(name = "priority")
        val priorityFeePerByte: Int? = null
)

@JsonClass(generateAdapter = true)
data class BlockchainInfoInput(
        val sequence: Long? = null,

        @Json(name = "prev_out")
        val prevOutput: BlockchainInfoOutput? = null
)

@JsonClass(generateAdapter = true)
data class BlockchainInfoOutput(
        val script: String? = null,

        @Json(name = "tx_index")
        val transactionIndex: Long? = null,

        @Json(name = "n")
        val index: Int? = null,

        val value: Long? = null,

        @Json(name = "addr")
        val address: String? = null
)