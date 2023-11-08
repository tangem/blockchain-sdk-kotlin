package com.tangem.blockchain.blockchains.bitcoin.network.blockchaininfo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BlockchainInfoAddress(
        @Json(name = "final_balance")
        val finalBalance: Long? = null,

        @Json(name = "n_tx")
        val transactionCount: Int? = null,

        @Json(name = "address")
        val address: String? = null,

        @Json(name = "txs")
        val transactions: List<BlockchainInfoTransaction>? = null,
)

@JsonClass(generateAdapter = true)
data class BlockchainInfoTransaction(
        val hash: String? = null,

        @Json(name = "block_height")
        val blockHeight: Long? = null,

        @Json(name = "result")
        val balanceDif: Long? = null,

        @Json(name = "vin_sz")
        val inputCount: Int? = null,

        val time: Long? = null,

        @Json(name = "inputs")
        val inputs: List<BlockchainInfoInput> = emptyList(),

        @Json(name = "outputs")
        val outputs: List<BlockchainInfoOutput> = emptyList(),
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
        val outputScript: String? = null,
)

@JsonClass(generateAdapter = true)
data class BlockchainInfoFees(
        @Json(name = "regular")
        val regularFeePerByte: Int? = null,

        @Json(name = "priority")
        val priorityFeePerByte: Int? = null,
)

@JsonClass(generateAdapter = true)
data class BlockchainInfoInput(
        @Json(name = "sequence")
        val sequence: Int?,

        @Json(name = "witness")
        val witness: String?,

        @Json(name = "script")
        val script: String?,

        @Json(name = "n")
        val index: Int?,

        @Json(name = "prev_out")
        val previousOutput: BlockchainInfoOutput?,
)

@JsonClass(generateAdapter = true)
data class BlockchainInfoOutput(
        @Json(name = "type")
        val type: Int?,

        @Json(name = "spent")
        val spent: Boolean?,

        @Json(name = "value")
        val value: Long?,

        @Json(name = "script")
        val script: String?,

        @Json(name = "addr")
        val address: String?,

        @Json(name = "n")
        val index: Int?,

        @Json(name = "tx_index")
        val txIndex: Long?,
)
