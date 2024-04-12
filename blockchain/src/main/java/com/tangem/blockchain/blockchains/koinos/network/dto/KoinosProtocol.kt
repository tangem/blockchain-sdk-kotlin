package com.tangem.blockchain.blockchains.koinos.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * @see <a href=https://github.com/koinos/koinos-proto/blob/master/koinos/rpc/chain/chain_rpc.proto>koinos/protocol/protocol.proto</a>
 */
internal object KoinosProtocol {

    /**
     * ```proto
     * message transaction {
     *    bytes id = 1 [(btype) = TRANSACTION_ID];
     *    transaction_header header = 2;
     *    repeated operation operations = 3;
     *    repeated bytes signatures = 4;
     * }
     *```
     */
    @JsonClass(generateAdapter = true)
    data class Transaction(
        @Json(name = "header") val header: TransactionHeader,
        @Json(name = "id") val id: String,
        @Json(name = "operations") val operations: List<Operation>,
        @Json(name = "signatures") val signatures: List<String>,
    )

    /**
     *```proto
     * message transaction_header {
     *    bytes chain_id = 1;
     *    uint64 rc_limit = 2 [jstype = JS_STRING];
     *    bytes nonce = 3;
     *    bytes operation_merkle_root = 4;
     *    bytes payer = 5 [(btype) = ADDRESS];
     *    bytes payee = 6 [(btype) = ADDRESS];
     * }
     * ```
     */
    @JsonClass(generateAdapter = true)
    data class TransactionHeader(
        @Json(name = "chain_id") val chainId: String,
        @Json(name = "rc_limit") val rcLimit: Long,
        @Json(name = "nonce") val nonce: String,
        @Json(name = "operation_merkle_root") val operationMerkleRoot: String,
        @Json(name = "payer") val payer: String,
        @Json(name = "payee") val payee: String? = null,
    )

    /**
     * ```protobuf
     * message operation {
     *    oneof op {
     *       upload_contract_operation upload_contract = 1;
     *       call_contract_operation call_contract = 2;
     *       set_system_call_operation set_system_call = 3;
     *       set_system_contract_operation set_system_contract = 4;
     *    }
     * }
     * ```
     */
    @JsonClass(generateAdapter = true)
    data class Operation(
        // upload_contract_operation upload_contract
        @Json(name = "call_contract")
        val callContract: CallContractOperation,
        // set_system_call_operation set_system_call
        // set_system_contract_operation set_system_contract
    )

    /**
     * ```protobuf
     * message call_contract_operation {
     *    bytes contract_id = 1 [(btype) = CONTRACT_ID];
     *    uint32 entry_point = 2;
     *    bytes args = 3;
     * }
     * ```
     */
    @JsonClass(generateAdapter = true)
    data class CallContractOperation(
        @Json(name = "contract_id")
        val contractIdBase58: String,
        @Json(name = "entry_point")
        val entryPoint: Int,
        @Json(name = "args")
        val argsBase64: String,
    )

    /**
     * ```protobuf
     * message transaction_receipt {
     *    bytes id = 1 [(btype) = TRANSACTION_ID];
     *    bytes payer = 2 [(btype) = ADDRESS];
     *    uint64 max_payer_rc = 3 [jstype = JS_STRING];
     *    uint64 rc_limit = 4 [jstype = JS_STRING];
     *    uint64 rc_used = 5 [jstype = JS_STRING];
     *    uint64 disk_storage_used = 6 [jstype = JS_STRING];
     *    uint64 network_bandwidth_used = 7 [jstype = JS_STRING];
     *    uint64 compute_bandwidth_used = 8 [jstype = JS_STRING];
     *    bool reverted = 9;
     *    repeated event_data events = 10;
     *    repeated string logs = 11;
     *    repeated state_delta_entry state_delta_entries = 12;
     * }
     * ```
     */
    @JsonClass(generateAdapter = true)
    data class TransactionReceipt(
        @Json(name = "id") val id: String,
        @Json(name = "payer") val payer: String = "",
        @Json(name = "max_payer_rc") val maxPayerRc: Long = 0L,
        @Json(name = "rc_limit") val rcLimit: Long = 0L,
        @Json(name = "rc_used") val rcUsed: Long = 0L,
        @Json(name = "disk_storage_used") val diskStorageUsed: String?,
        @Json(name = "network_bandwidth_used") val networkBandwidthUsed: String?,
        @Json(name = "compute_bandwidth_used") val computeBandwidthUsed: String?,
        @Json(name = "reverted") val reverted: Boolean?,
        @Json(name = "events") val events: List<EventData> = emptyList(),
        // field: logs
        // field: state_delta_entries
    )

    /**
     * ```protobuf
     * message block_header {
     *    bytes previous = 1 [(btype) = BLOCK_ID];
     *    uint64 height = 2 [jstype = JS_STRING];
     *    uint64 timestamp = 3 [jstype = JS_STRING];
     *    bytes previous_state_merkle_root = 4;
     *    bytes transaction_merkle_root = 5;
     *    bytes signer = 6 [(btype) = ADDRESS];
     *    repeated bytes approved_proposals = 7 [(btype) = TRANSACTION_ID];
     * }
     * ```
     */
    @JsonClass(generateAdapter = true)
    data class BlockHeader(
        @Json(name = "previous") val previous: String,
        @Json(name = "height") val height: Long,
        @Json(name = "timestamp") val timestamp: Long,
        @Json(name = "previous_state_merkle_root") val previousStateMerkleRoot: String,
        @Json(name = "transaction_merkle_root") val transactionMerkleRoot: String,
        @Json(name = "signer") val signer: String,
        @Json(name = "approved_proposals") val approvedProposals: List<String>,
    )

    /**
     * ```protobuf
     * message block_receipt {
     *    bytes id = 1 [(btype) = BLOCK_ID];
     *    uint64 height = 2 [jstype = JS_STRING];
     *    uint64 disk_storage_used = 3 [jstype = JS_STRING];
     *    uint64 network_bandwidth_used = 4 [jstype = JS_STRING];
     *    uint64 compute_bandwidth_used = 5 [jstype = JS_STRING];
     *    bytes state_merkle_root = 6;
     *    repeated event_data events = 7;
     *    repeated transaction_receipt transaction_receipts = 8;
     *    repeated string logs = 9;
     *    uint64 disk_storage_charged = 10 [jstype = JS_STRING];
     *    uint64 network_bandwidth_charged = 11 [jstype = JS_STRING];
     *    uint64 compute_bandwidth_charged = 12 [jstype = JS_STRING];
     *    repeated state_delta_entry state_delta_entries = 13;
     * }
     * ```
     */
    @JsonClass(generateAdapter = true)
    data class BlockReceipt(
        @Json(name = "id") val id: String,
        @Json(name = "height") val height: Long,
        @Json(name = "disk_storage_used") val diskStorageUsed: String,
        @Json(name = "network_bandwidth_used") val networkBandwidthUsed: String,
        @Json(name = "compute_bandwidth_used") val computeBandwidthUsed: String,
        @Json(name = "state_merkle_root") val stateMerkleRoot: String,
        @Json(name = "events") val events: List<EventData>,
        @Json(name = "transaction_receipts") val transactionReceipts: List<TransactionReceipt>,
        // logs
        // disk_storage_charged
        // network_bandwidth_charged
        // compute_bandwidth_charged
    )

    /**
     * ```protobuf
     * message event_data {
     *    uint32 sequence = 1;
     *    bytes source = 2 [(btype) = CONTRACT_ID];
     *    string name = 3;
     *    bytes data = 4;
     *    repeated bytes impacted = 5 [(btype) = ADDRESS];
     * }
     * ```
     */
    @JsonClass(generateAdapter = true)
    data class EventData(
        @Json(name = "sequence") val sequence: Int?,
        @Json(name = "source") val source: String,
        @Json(name = "name") val name: String,
        @Json(name = "data") val eventData: String,
        @Json(name = "impacted") val impacted: List<String>,
    )
}