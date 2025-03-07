package com.tangem.blockchain.transactionhistory.blockchains.kaspa.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents a transaction on the Kaspa blockchain
 */
@JsonClass(generateAdapter = true)
internal data class KaspaCoinTransaction(
    @Json(name = "subnetwork_id")
    val subnetworkId: String,

    @Json(name = "transaction_id")
    val transactionId: String,

    @Json(name = "hash")
    val hash: String,

    @Json(name = "mass")
    val mass: String,

    @Json(name = "payload")
    val payload: String? = null,

    @Json(name = "block_hash")
    val blockHash: List<String>,

    @Json(name = "block_time")
    val blockTime: Long,

    @Json(name = "is_accepted")
    val isAccepted: Boolean,

    @Json(name = "accepting_block_hash")
    val acceptingBlockHash: String?,

    @Json(name = "accepting_block_blue_score")
    val acceptingBlockBlueScore: Long?,

    @Json(name = "inputs")
    val inputs: List<KaspaCoinTransactionInput> = emptyList(),

    @Json(name = "outputs")
    val outputs: List<KaspaTransactionTransactionOutput> = emptyList(),
)

/**
 * Represents an input in a Kaspa transaction
 */
@JsonClass(generateAdapter = true)
internal data class KaspaCoinTransactionInput(
    @Json(name = "transaction_id")
    val transactionId: String,

    @Json(name = "index")
    val index: Int,

    @Json(name = "previous_outpoint_hash")
    val previousOutpointHash: String,

    @Json(name = "previous_outpoint_index")
    val previousOutpointIndex: String,

    @Json(name = "previous_outpoint_resolved")
    val previousOutpointResolved: Any? = null,

    @Json(name = "previous_outpoint_address")
    val previousOutpointAddress: String,

    @Json(name = "previous_outpoint_amount")
    val previousOutpointAmount: Long,

    @Json(name = "signature_script")
    val signatureScript: String,

    @Json(name = "sig_op_count")
    val sigOpCount: String,
)

/**
 * Represents an output in a Kaspa transaction
 */
@JsonClass(generateAdapter = true)
internal data class KaspaTransactionTransactionOutput(
    @Json(name = "transaction_id")
    val transactionId: String,

    @Json(name = "index")
    val index: Int,

    @Json(name = "amount")
    val amount: Long,

    @Json(name = "script_public_key")
    val scriptPublicKey: String,

    @Json(name = "script_public_key_address")
    val scriptPublicKeyAddress: String,

    @Json(name = "script_public_key_type")
    val scriptPublicKeyType: String,

    @Json(name = "accepting_block_hash")
    val acceptingBlockHash: String? = null,
)