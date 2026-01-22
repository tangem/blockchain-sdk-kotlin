package com.tangem.blockchain.blockchains.ethereum.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response model for eth_getTransactionByHash
 * @see <a href="https://ethereum.org/developers/docs/apis/json-rpc/#eth_gettransactionbyhash">Ethereum JSON-RPC API</a>
 */
@JsonClass(generateAdapter = true)
internal data class EthereumTransactionResponse(
    @Json(name = "blockHash") val blockHash: String?,
    @Json(name = "blockNumber") val blockNumber: String?, // null when pending, hex string when executed
    @Json(name = "from") val from: String,
    @Json(name = "gas") val gas: String,
    @Json(name = "gasPrice") val gasPrice: String?,
    @Json(name = "hash") val hash: String,
    @Json(name = "input") val input: String,
    @Json(name = "nonce") val nonce: String,
    @Json(name = "to") val to: String?,
    @Json(name = "transactionIndex") val transactionIndex: String?,
    @Json(name = "value") val value: String,
    @Json(name = "v") val v: String?,
    @Json(name = "r") val r: String?,
    @Json(name = "s") val s: String?,
) {
    /**
     * Returns true if transaction is executed (has blockNumber)
     */
    fun isExecuted(): Boolean = blockNumber != null

    /**
     * Returns true if transaction is still pending (result exists but blockNumber is null)
     */
    fun isPending(): Boolean = blockNumber == null
}