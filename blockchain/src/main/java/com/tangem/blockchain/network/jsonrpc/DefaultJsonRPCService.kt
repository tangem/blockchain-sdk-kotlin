package com.tangem.blockchain.network.jsonrpc

import com.tangem.blockchain.common.JsonRPCRequest
import com.tangem.blockchain.common.JsonRPCResponse
import com.tangem.blockchain.network.createRetrofitInstance
import okhttp3.OkHttpClient

internal class DefaultJsonRPCService(
    url: String,
    okHttpClient: OkHttpClient,
) : JsonRPCService {

    private val service = createRetrofitInstance(url)
        .newBuilder()
        .client(okHttpClient)
        .build()
        .create(JsonRPCServiceApi::class.java)

    override suspend fun call(jsonRPCRequest: JsonRPCRequest): Result<JsonRPCResponse> {
        return runCatching { service.call(jsonRPCRequest) }
    }
}