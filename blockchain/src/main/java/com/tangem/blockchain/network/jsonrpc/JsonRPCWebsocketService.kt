package com.tangem.blockchain.network.jsonrpc

import com.tangem.blockchain.common.JsonRPCRequest
import com.tangem.blockchain.common.JsonRPCResponse
import kotlinx.coroutines.flow.StateFlow

/**
 * Service for communicating with node via websocket JsonRPC
 * When connecting with keepAlive == false, the connection will be held until the timer expires
 * Otherwise, the connection will be active until the disconnect method is called or any connection error occurs.
 */
internal interface JsonRPCWebsocketService {

    /**
     * Represents current connection state
     * ESTABLISHED - ready to accept requests
     * DISCONNECTED - no websocket connection
     */
    val state: StateFlow<WebSocketConnectionStatus>

    /**
     * Establish websocket connection
     * Synchronized method - returns after full connection
     *
     * Does nothing if connection is already established
     *
     * @param keepAlive true - the connection is being hold all the time,
     * false - the connection will be terminated some time after the last call
     * @throws Exception if no connection
     */
    suspend fun connect(keepAlive: Boolean = false)

    /**
     * Terminate connection
     * All [call] will be cancelled
     */
    suspend fun disconnect()

    /**
     * Synchronized exception free JsonRPC method call.
     *
     * In case the connection is not established, it will be established with keepAlive = false
     *
     * If the connection was established with keepAlive == false,
     * the call will reset the timer, after which the connection will be closed
     *
     * @param jsonRPCRequest JsonRPC request
     * @return the result of the request or websocket connection error
     */
    suspend fun call(jsonRPCRequest: JsonRPCRequest): Result<JsonRPCResponse>
}
