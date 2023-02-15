package com.tangem.blockchain.network.blockscout

import com.squareup.moshi.Json
import retrofit2.http.GET
import retrofit2.http.Query

/**
[REDACTED_AUTHOR]
 */
interface BlockscoutApi {

    @GET("api?module=account&action=listaccounts")
    suspend fun getAccountsList(): BlockscoutResponse<BlockscoutAccount>

    @GET("api?module=account&action=txlist")
    suspend fun getTransactionsList(
        @Query("address") address: String,
    ): BlockscoutResponse<List<BlockscoutTransaction>>

    @GET("api?module=account&action=tokentx")
    suspend fun getTokenTransactionsList(
        @Query("address") address: String,
    ): BlockscoutResponse<List<BlockscoutTransaction>>

    @GET("api?module=transaction&action=gettxinfo")
    suspend fun getTransactionInfo(
        @Query("txhash") txHash: String,
    ): BlockscoutResponse<BlockscoutTransactionInfo>
}

open class BlockscoutResponse<T>(
    val message: String,
    val status: Int,
    val result: T?,
)


data class BlockscoutTransaction(
    @Json(name = "blockHash") val blockHash: String,
    @Json(name = "blockNumber") val blockNumber: String,
    @Json(name = "confirmations") val confirmations: String,
    @Json(name = "contractAddress") val contractAddress: String,
    @Json(name = "cumulativeGasUsed") val cumulativeGasUsed: String,
    @Json(name = "from") val from: String,
    @Json(name = "gas") val gas: String,
    @Json(name = "gasPrice") val gasPrice: String,
    @Json(name = "gasUsed") val gasUsed: String,
    @Json(name = "hash") val hash: String,
    @Json(name = "input") val input: String,
    @Json(name = "nonce") val nonce: String,
    @Json(name = "timeStamp") val timeStamp: String,
    @Json(name = "to") val to: String,
    @Json(name = "transactionIndex") val transactionIndex: String,
    @Json(name = "value") val value: String,
    @Json(name = "isError") val isError: String?,
    @Json(name = "txreceipt_status") val transactionReceiptStatus: String?,
)

data class BlockscoutAccount(
    @Json(name = "address") val address: String,
    @Json(name = "balance") val balance: String,
    @Json(name = "stale") val stale: Boolean,
)

data class BlockscoutTransactionInfo(
    @Json(name = "blockNumber") val blockNumber: String,
    @Json(name = "confirmations") val confirmations: String,
    @Json(name = "from") val from: String,
    @Json(name = "gasLimit") val gasLimit: String,
    @Json(name = "gasPrice") val gasPrice: String,
    @Json(name = "gasUsed") val gasUsed: String,
    @Json(name = "hash") val hash: String,
    @Json(name = "input") val input: String,
    @Json(name = "logs") val logs: List<Any>,
    @Json(name = "next_page_params") val nextPageParams: Any?,
    @Json(name = "revertReason") val revertReason: String,
    @Json(name = "success") val success: Boolean,
    @Json(name = "timeStamp") val timeStamp: String,
    @Json(name = "to") val to: String,
    @Json(name = "value") val value: String,
)