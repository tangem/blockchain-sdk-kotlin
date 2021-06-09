package com.tangem.blockchain.blockchains.ethereum.network

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

interface EthereumApi {
    @Headers("Content-Type: application/json")
    @POST()
    suspend fun post(@Body body: EthereumBody?, @Url infuraProjectId: String): EthereumResponse
}

@JsonClass(generateAdapter = true)
data class EthereumBody(
        val method: String,
        val params: List<Any> = listOf(),
        val jsonrpc: String = "2.0",
        val id: Int = 67
)

data class EthCallObject(val to: String, val from: String? = null, val data: String? = null)

enum class EthereumMethod(val value: String) {
    GET_BALANCE("eth_getBalance"),
    GET_TRANSACTION_COUNT("eth_getTransactionCount"),
    CALL("eth_call"),
    SEND_RAW_TRANSACTION("eth_sendRawTransaction"),
    ESTIMATE_GAS("eth_estimateGas"),
    GAS_PRICE("eth_gasPrice")
}

enum class EthBlockParam(val value: String) {
    EARLIEST("earliest"),
    LATEST("latest"),
    PENDING("pending")
}
