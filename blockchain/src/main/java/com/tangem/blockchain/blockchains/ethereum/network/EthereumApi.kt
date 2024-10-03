package com.tangem.blockchain.blockchains.ethereum.network

import com.tangem.blockchain.common.JsonRPCRequest
import com.tangem.blockchain.common.JsonRPCResponse
import com.tangem.blockchain.common.NowNodeCredentials
import retrofit2.http.*

internal interface EthereumApi {

    @Headers("Content-Type: application/json")
    @POST
    suspend fun post(
        @Body body: JsonRPCRequest,
        @Url infuraProjectId: String,
        @Header("Authorization") token: String? = null,
        @Header(NowNodeCredentials.headerApiKey) nowNodesApiKey: String? = null,
    ): JsonRPCResponse
}

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
    FEE_HISTORY("eth_feeHistory"),
}

enum class EthBlockParam(val value: String) {
    EARLIEST("earliest"),
    LATEST("latest"),
    PENDING("pending"),
}