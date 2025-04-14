package com.tangem.blockchain.network.jsonrpc

import com.tangem.blockchain.common.JsonRPCRequest
import com.tangem.blockchain.common.JsonRPCResponse

internal interface JsonRPCService {

    /**
     * Synchronized exception free JsonRPC method call.
     *
     * @param jsonRPCRequest JsonRPC request
     * @return the result of the request or websocket connection error
     */
    suspend fun call(jsonRPCRequest: JsonRPCRequest): Result<JsonRPCResponse>
}