package com.tangem.blockchain.blockchains.near.network.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal
import java.math.BigInteger

/**
 * @author Anton Zhilenkov on 08.08.2023.
 */

data class ProtocolConfigResult(
    @Json(name = "chain_id") val chainId: String,
    @Json(name = "protocol_version") val protocolVersion: String,
    @Json(name = "genesis_height") val genesisHeight: Long,
    @Json(name = "max_gas_price") val maxGasPrice: BigDecimal,
    @Json(name = "min_gas_price") val minGasPrice: BigDecimal,
    @Json(name = "runtime_config") val runtimeConfig: RuntimeConfig,
) {
    data class RuntimeConfig(
        @Json(name = "transaction_costs") val transactionCosts: TransactionCost,
        @Json(name = "storage_amount_per_byte") val storageAmountPerByte: BigDecimal,
    )

    data class TransactionCost(
        @Json(name = "action_creation_config") val actionCreationConfig: ActionCreationConfig,
        @Json(name = "action_receipt_creation_config") val actionReceiptCreationConfig: CostConfig,
    )

    data class ActionCreationConfig(
        @Json(name = "add_key_cost") val addKeyCost: AddKeyCost,
        @Json(name = "transfer_cost") val transferCost: CostConfig,
        @Json(name = "create_account_cost") val createAccountCost: CostConfig,
    )

    data class AddKeyCost(
        @Json(name = "full_access_cost") val fullAccessCost: CostConfig,
        @Json(name = "function_call_cost") val functionCallCost: CostConfig,
    )

    data class CostConfig(
        @Json(name = "send_sir") val sendSir: Long,
        @Json(name = "send_not_sir") val sendNotSir: Long,
        @Json(name = "execution") val execution: Long,
    )
}

@JsonClass(generateAdapter = true)
data class NetworkStatusResult(
    @Json(name = "chain_id") val chainId: String,
    @Json(name = "latest_protocol_version") val latestProtocolVersion: Int,
    @Json(name = "node_key") val nodeKey: Any?,
    @Json(name = "node_public_key") val nodePublicKey: String,
    @Json(name = "protocol_version") val protocolVersion: Int,
    @Json(name = "rpc_addr") val rpcIpAddress: String,
    @Json(name = "uptime_sec") val uptimeSeconds: Long,
    @Json(name = "validator_account_id") val validatorAccountId: Any?,
    @Json(name = "validator_public_key") val validatorPublicKey: Any?,
    @Json(name = "sync_info") val syncInfo: SyncInfo,
    @Json(name = "version") val version: Version,
) {
    data class SyncInfo(
        @Json(name = "earliest_block_hash") val earliestBlockHash: String,
        @Json(name = "earliest_block_height") val earliestBlockHeight: Long,
        @Json(name = "earliest_block_time") val earliestBlockTime: String,
        @Json(name = "epoch_id") val epochId: String,
        @Json(name = "epoch_start_height") val epochStartHeight: Long,
        @Json(name = "latest_block_hash") val latestBlockHash: String,
        @Json(name = "latest_block_height") val latestBlockHeight: Long,
        @Json(name = "latest_block_time") val latestBlockTime: String,
        @Json(name = "latest_state_root") val latestStateRoot: String,
        @Json(name = "syncing") val syncing: Boolean,
    )

    data class Version(
        @Json(name = "build") val build: String,
        @Json(name = "rustc_version") val rustcVersion: String,
        @Json(name = "version") val version: String,
    )
}

@JsonClass(generateAdapter = true)
data class AccessKeyResult(
    @Json(name = "nonce") val nonce: Long,
    @Json(name = "block_height") val blockHeight: Long,
    @Json(name = "block_hash") val blockHash: String,
    @Json(name = "permission") val permission: Any,
)

@JsonClass(generateAdapter = true)
data class ViewAccountResult(
    @Json(name = "amount") val amount: String,
    @Json(name = "locked") val locked: String,
    @Json(name = "code_hash") val codeHash: String,
    @Json(name = "storage_usage") val storageUsage: Int,
    @Json(name = "storage_paid_at") val storagePaidAt: Int,
    @Json(name = "block_height") val blockHeight: Long,
    @Json(name = "block_hash") val blockHash: String,
)

@JsonClass(generateAdapter = true)
data class GasPriceResult(
    @Json(name = "gas_price") val gasPrice: String,
)

typealias SendTransactionAsyncResult = String

@JsonClass(generateAdapter = true)
data class TransactionStatusResult(
    @Json(name = "status") val status: Status,
    @Json(name = "transaction") val transaction: Transaction,
) {

    @JsonClass(generateAdapter = true)
    data class Status(
        @Json(name = "SuccessValue") val successValue: String?,
    )

    @JsonClass(generateAdapter = true)
    data class Transaction(
        @Json(name = "signer_id") val signerId: String,
        @Json(name = "public_key") val publicKey: String,
        @Json(name = "nonce") val nonce: Long,
        @Json(name = "receiver_id") val receiverId: String,
        @Json(name = "signature") val signature: String,
        @Json(name = "hash") val hash: String,
    )

    @JsonClass(generateAdapter = true)
    data class Outcome(
        // @Json(name = "proof") val proof: List<Proof>,
        @Json(name = "block_hash") val blockHash: String,
        @Json(name = "id") val id: String,
        @Json(name = "outcome") val outcome: OutcomeData,
    )

    @JsonClass(generateAdapter = true)
    data class Proof(
        @Json(name = "hash") val hash: String,
        @Json(name = "direction") val direction: String,
    )

    @JsonClass(generateAdapter = true)
    data class OutcomeData(
        @Json(name = "receipt_ids") val receiptIds: List<String>,
        @Json(name = "gas_burnt") val gasBurnt: Double,
        @Json(name = "tokens_burnt") val tokensBurnt: String,
        @Json(name = "status") val status: Any,
    )
}