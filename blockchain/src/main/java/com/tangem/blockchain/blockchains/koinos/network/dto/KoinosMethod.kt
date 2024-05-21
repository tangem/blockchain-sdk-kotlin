package com.tangem.blockchain.blockchains.koinos.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.blockchain.common.JsonRPCRequest
import com.tangem.common.extensions.toByteArray
import koinos.chain.value_type
import okio.ByteString.Companion.decodeBase64
import org.kethereum.extensions.toBigInteger
import java.math.BigInteger

/**
 * Koinos JSON-RPC methods
 */
internal sealed interface KoinosMethod {

    fun asRequest(): JsonRPCRequest

    /**
     * ```protobuf
     * message get_account_rc_request {
     *    bytes account = 1 [(btype) = ADDRESS];
     * }
     * ```
     * @see <a href=https://github.com/koinos/koinos-proto/blob/master/koinos/rpc/chain/chain_rpc.proto>koinos/rpc/chain/chain_rpc.proto</a>
     */
    @JsonClass(generateAdapter = true)
    data class GetAccountRC(
        @Json(name = "account") val account: String,
    ) : KoinosMethod {
        override fun asRequest() = request(method = "chain.get_account_rc")

        /**
         * ```protobuf
         * message get_account_rc_response {
         *    uint64 rc = 1 [jstype = JS_STRING];
         * }
         * ```
         */
        @JsonClass(generateAdapter = true)
        data class Response(
            @Json(name = "rc") val rc: Long = 0L,
        )
    }

    /**
     * ```protobuf
     * message get_account_nonce_request {
     *    bytes account = 1 [(btype) = ADDRESS];
     * }
     * ```
     * @see <a href=https://github.com/koinos/koinos-proto/blob/master/koinos/rpc/chain/chain_rpc.proto>koinos/rpc/chain/chain_rpc.proto</a>
     */
    @JsonClass(generateAdapter = true)
    data class GetAccountNonce(
        @Json(name = "account") val account: String,
    ) : KoinosMethod {
        override fun asRequest() = request("chain.get_account_nonce")

        /**
         * ```protobuf
         * message get_account_nonce_response {
         *    bytes nonce = 1;
         * }
         * ```
         */
        @JsonClass(generateAdapter = true)
        data class Response(
            @Json(name = "nonce") val nonce: String,
        ) {
            val nonceTypeName = "koinos.chai.value_type"

            fun decode(): BigInteger? {
                return value_type.ADAPTER.decode(
                    nonce.decodeBase64() ?: return null,
                ).uint64_value?.toByteArray()?.toBigInteger()
            }
        }
    }

    /**
     * ```protobuf
     * message get_account_history_request {
     *    bytes address = 1 [(btype) = ADDRESS];
     *    optional uint64 seq_num = 2 [jstype = JS_STRING];
     *    uint64 limit = 3 [jstype = JS_STRING];
     *    bool ascending = 4;
     *    bool irreversible = 5;
     * }
     * ```
     * @see <a href=https://github.com/koinos/koinos-proto/blob/master/koinos/rpc/account_history/account_history_rpc.proto>koinos/rpc/account_history/account_history_rpc.proto</a>
     */
    @JsonClass(generateAdapter = true)
    data class GetAccountHistory(
        @Json(name = "address") val address: String,
        @Json(name = "seq_num") val seqNumber: String? = null,
        @Json(name = "limit") val limit: String,
        @Json(name = "ascending") val ascending: Boolean,
        @Json(name = "irreversible") val irreversible: Boolean,
    ) : KoinosMethod {
        override fun asRequest() = request(method = "account_history.get_account_history")

        /**
         * ```protobuf
         * message get_account_history_response {
         *    repeated account_history_entry values = 1;
         * }
         * ```
         */
        @JsonClass(generateAdapter = true)
        data class Response(
            @Json(name = "values")
            val values: List<KoinosAccountHistory.AccountHistoryEntry>,
        )
    }

    // TODO
    // {"id":0,"jsonrpc":"2.0","method":"chain.read_contract","params":{"contract_id":"1FaSvLjQJsCJKq5ybmGsMMQs8RQYyVv8ju","entry_point":1550980247,"args":"ChkAaEFbbHucCFnoEOh3RgGrOZ38TNTI9xMW"}}

    /**
     * ```protobuf
     * message read_contract_request {
     *    bytes contract_id = 1 [(btype) = CONTRACT_ID];
     *    uint32 entry_point = 2;
     *    bytes args = 3;
     * }
     * ```
     * @see <a href=https://github.com/koinos/koinos-proto/blob/master/koinos/rpc/chain/chain_rpc.proto>koinos/rpc/chain/chain_rpc.proto</a>
     */
    @JsonClass(generateAdapter = true)
    data class ReadContract(
        @Json(name = "contract_id") val contractId: String,
        @Json(name = "entry_point") val entryPoint: Int,
        @Json(name = "args") val args: String,
    ) : KoinosMethod {

        override fun asRequest() = request(method = "chain.read_contract")

        /**
         * message read_contract_response {
         *    bytes result = 1;
         *    repeated string logs = 2;
         * }
         */
        @JsonClass(generateAdapter = true)
        data class Response(
            @Json(name = "result") val result: String?,
            // repeated string logs = 2;
        )
    }

    /**
     * ```protobuf
     * message submit_transaction_request {
     *    protocol.transaction transaction = 1;
     *    bool broadcast = 2;
     * }
     * ```
     * @see <a href=https://github.com/koinos/koinos-proto/blob/master/koinos/rpc/chain/chain_rpc.proto>koinos/rpc/chain/chain_rpc.proto</a>
     */
    @JsonClass(generateAdapter = true)
    data class SubmitTransaction(
        @Json(name = "transaction") val transaction: KoinosProtocol.Transaction,
        @Json(name = "broadcast") val broadcast: Boolean,
    ) : KoinosMethod {

        override fun asRequest() = request("chain.submit_transaction")

        /**
         * ```protobuf
         * message submit_transaction_response {
         *    protocol.transaction_receipt receipt = 1;
         * }
         * ```
         */
        @JsonClass(generateAdapter = true)
        data class Response(
            @Json(name = "receipt") val receipt: KoinosProtocol.TransactionReceipt,
        )
    }

    /**
     * ```protobuf
     * message get_resource_limits_request {}
     * ```
     *
     * @see <a href=https://github.com/koinos/koinos-proto/blob/master/koinos/rpc/chain/chain_rpc.proto>koinos/rpc/chain/chain_rpc.proto</a>
     */
    object GetResourceLimits : KoinosMethod {
        override fun asRequest(): JsonRPCRequest = JsonRPCRequest(
            method = "chain.get_resource_limits",
            params = null,
        )

        @JsonClass(generateAdapter = true)
        data class Response(
            @Json(name = "resource_limit_data") val resourceLimitData: KoinosChain.ResourceLimitData,
        )
    }
}

private fun KoinosMethod.request(method: String) = JsonRPCRequest(
    method = method,
    params = this,
)