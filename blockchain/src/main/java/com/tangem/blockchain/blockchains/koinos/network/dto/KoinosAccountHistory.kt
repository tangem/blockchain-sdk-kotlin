package com.tangem.blockchain.blockchains.koinos.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * @see <a href=https://github.com/koinos/koinos-proto/blob/master/koinos/account_history/account_history.proto>koinos/account_history/account_history.proto</a>
 */
internal object KoinosAccountHistory {

    /**
     * ```protobuf
     * message account_history_entry {
     *    uint64 seq_num = 1 [jstype = JS_STRING];
     *    oneof record {
     *       .koinos.account_history.transaction_record trx = 2;
     *       .koinos.account_history.block_record block = 3;
     *    }
     * }
     * ```
     *
     * @see <a href=https://github.com/koinos/koinos-proto/blob/master/koinos/rpc/account_history/account_history_rpc.proto>koinos/rpc/account_history/account_history_rpc.proto</a>
     */
    @JsonClass(generateAdapter = true)
    data class AccountHistoryEntry(
        @Json(name = "seq_num")
        val seqNum: Long = 0L,
        @Json(name = "trx")
        val transaction: TransactionRecord?,
        @Json(name = "block")
        val block: BlockRecord?,
    )

    /**
     *```protobuf
     * message block_record {
     *    protocol.block_header header = 1;
     *    protocol.block_receipt receipt = 2;
     * }
     * ```
     */
    @JsonClass(generateAdapter = true)
    data class BlockRecord(
        @Json(name = "header")
        val header: KoinosProtocol.BlockHeader,
        @Json(name = "receipt")
        val receipt: KoinosProtocol.BlockReceipt,
    )

    /**
     * ```protobuf
     * message transaction_record {
     *    protocol.transaction transaction = 1;
     *    protocol.transaction_receipt receipt = 2;
     * }
     * ```
     */
    @JsonClass(generateAdapter = true)
    data class TransactionRecord(
        @Json(name = "transaction") val transaction: KoinosProtocol.Transaction,
        @Json(name = "receipt") val receipt: KoinosProtocol.TransactionReceipt,
    )
}