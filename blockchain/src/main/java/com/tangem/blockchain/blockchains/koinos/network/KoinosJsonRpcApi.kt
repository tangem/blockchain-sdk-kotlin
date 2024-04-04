package com.tangem.blockchain.blockchains.koinos.network

import com.tangem.blockchain.common.JsonRPCRequest
import com.tangem.blockchain.common.JsonRPCResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Koinos JSON-RPC interface
 * @see <a href="https://docs.koinos.io/rpc/json-rpc/">Koinos JSON-RPC docs</a>
 */
internal interface KoinosJsonRpcApi {
    @POST
    suspend fun send(@Header("apikey") apiKey: String? = null, @Body body: JsonRPCRequest): JsonRPCResponse
}