package com.tangem.blockchain.blockchains.ethereum.network

import com.squareup.moshi.JsonClass
import com.tangem.blockchain.common.NowNodeCredentials
import retrofit2.http.*

interface EthereumApi {
    @Headers("Content-Type: application/json")
    @POST
    suspend fun post(
        @Body body: EthereumBody?,
        @Url infuraProjectId: String,
        @Header("Authorization") token: String? = null,
        @Header(NowNodeCredentials.headerApiKey) nowNodesApiKey: String? = null,
    ): EthereumResponse
}

@JsonClass(generateAdapter = true)
data class EthereumBody(
    val method: String,
    val params: List<Any> = listOf(),
    val jsonrpc: String = "2.0",
    val id: Int = 67,
)

data class EthCallObject(
    val to: String,
    val from: String? = null,
    val value: String? = null,
    val data: String? = null,
)

enum class EthereumMethod(val value: String) {
    GET_BALANCE("eth_getBalance"),
    GET_TRANSACTION_COUNT("eth_getTransactionCount"),
    CALL("eth_call"),
    SEND_RAW_TRANSACTION("eth_sendRawTransaction"),
    ESTIMATE_GAS("eth_estimateGas"),
    GAS_PRICE("eth_gasPrice"),
}

enum class EthBlockParam(val value: String) {
    EARLIEST("earliest"),
    LATEST("latest"),
    PENDING("pending"),
}
