package com.tangem.blockchain.blockchains.near.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
[REDACTED_AUTHOR]
 */
@JsonClass(generateAdapter = true)
data class NearResponse<Result>(
    @Json(name = "jsonrpc") val jsonRpc: String,
    @Json(name = "id") val id: String,
    @Json(name = "result") val result: Result?,
    @Json(name = "error") val error: ErrorResult?,
) {

    @JsonClass(generateAdapter = true)
    data class ErrorResult(
        @Json(name = "name") val name: String,
        @Json(name = "cause") val cause: Cause,
        @Json(name = "code") val code: Int,
        @Json(name = "data") val data: String,
        @Json(name = "message") val message: String,
    ) {

        @JsonClass(generateAdapter = true)
        data class Cause(
            @Json(name = "info") val info: Any,
            @Json(name = "name") val name: String,
        )
    }
}

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
    @Json(name = "transaction_outcome") val transactionOutcome: Outcome,
    @Json(name = "receipts_outcome") val receiptsOutcome: Outcome,
) {

    @JsonClass(generateAdapter = true)
    data class Status(
        @Json(name = "SuccessValue") val successValue: String,
    )

    @JsonClass(generateAdapter = true)
    data class Transaction(
        @Json(name = "signer_id") val signerId: String,
        @Json(name = "public_key") val publicKey: String,
        @Json(name = "nonce") val nonce: Int,
        @Json(name = "receiver_id") val receiverId: String,
        @Json(name = "actions") val actions: List<Any>,
        @Json(name = "signature") val signature: String,
        @Json(name = "hash") val hash: String,
    )

    @JsonClass(generateAdapter = true)
    data class Outcome(
        @Json(name = "proof") val proof: List<Proof>,
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
        @Json(name = "logs") val logs: List<Any>,
        @Json(name = "receipt_ids") val receiptIds: List<String>,
        @Json(name = "gas_burnt") val gasBurnt: Double,
        @Json(name = "tokens_burnt") val tokensBurnt: String,
        @Json(name = "executor_id") val executorId: String,
        @Json(name = "status") val status: Any,
    )
}