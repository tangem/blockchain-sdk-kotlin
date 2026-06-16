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

/**
 * Request bundle for `eth_estimateGas` with the JSON-RPC `stateOverride` (a.k.a. `stateDiff`)
 * third parameter. See [com.tangem.blockchain.blockchains.ethereum.gas.StateOverrideBuilder].
 */
internal data class EthereumStateOverrideEstimateGasRequest(
    val call: EthCallObject,
    val stateOverride: Map<String, Map<String, Map<String, String>>>,
)

enum class EthBlockParam(val value: String) {
    EARLIEST("earliest"),
    LATEST("latest"),
    PENDING("pending"),
}