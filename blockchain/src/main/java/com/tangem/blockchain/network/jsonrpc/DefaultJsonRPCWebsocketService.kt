package com.tangem.blockchain.network.jsonrpc

import com.tangem.blockchain.common.JsonRPCRequest
import com.tangem.blockchain.common.JsonRPCRequestJsonAdapter
import com.tangem.blockchain.common.JsonRPCResponse
import com.tangem.blockchain.common.JsonRPCResponseJsonAdapter
import com.tangem.blockchain.network.moshi
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*

private const val PING_INTERVAL_MILLIS = 10000L
private const val REQUEST_TIMEOUT_MILLIS = 30000L
private const val DISCONNECT_TIMER_MILLIS = 60000L
private const val WEBSOCKET_CONNECTION_CLOSE_NORMAL_STATUS = 1000

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultJsonRPCWebsocketService(
    private val wssUrl: String,
    private val pingPongRequestFactory: () -> JsonRPCRequest,
    private val okHttpClient: OkHttpClient = OkHttpClient(),
) : JsonRPCWebsocketService {

    private val jsonRequestAdapter = JsonRPCRequestJsonAdapter(moshi)
    private val jsonResponseAdapter = JsonRPCResponseJsonAdapter(moshi)

    override val state = MutableStateFlow(WebSocketConnectionStatus.DISCONNECTED)

    private val responseMessages = MutableSharedFlow<JsonRPCResponse>(
        replay = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var disconnectTimerJob: Job? = null
    private val requests = MutableStateFlow<List<Deferred<JsonRPCResponse>>>(emptyList())
    private var socket: WebSocket? = null
    private val keepAlive = MutableStateFlow(false)
    private val mutex = Mutex()

    override suspend fun connect(keepAlive: Boolean) = mutex.withLock {
        if (state.value == WebSocketConnectionStatus.ESTABLISHED) {
            return@withLock
        }

        this.keepAlive.value = keepAlive

        val request = Request.Builder()
            .url(wssUrl)
            .build()

        // establish connection
        socket = okHttpClient.newWebSocket(
            request,
            InnerWebSocketListener(),
        )

        // wait the connection to be established
        withTimeout(REQUEST_TIMEOUT_MILLIS) {
            state.first { it == WebSocketConnectionStatus.ESTABLISHED }
        }

        // start ping-pong
        coroutineScope.launch {
            pingPong()
        }
    }

    private suspend fun pingPong() {
        flow {
            delay(PING_INTERVAL_MILLIS)
            while (true) {
                emit(Unit)
                delay(PING_INTERVAL_MILLIS)
            }
        }.collectLatest {
            /**
             * Request example
             *
             * JsonRPCRequest(
             *   method = "server.ping",
             *   id = "keepAlive",
             *   params = emptyList<String>()
             * )
             */
            socket?.send(jsonRequestAdapter.toJson(pingPongRequestFactory()))
        }
    }

    private fun refreshDisconnectTimer() {
        disconnectTimerJob?.cancel()
        disconnectTimerJob = coroutineScope.launch {
            delay(DISCONNECT_TIMER_MILLIS)
            disconnect()
        }
    }

    override suspend fun disconnect() {
        // cancel disconnect timer and ping-pong
        coroutineScope.coroutineContext.cancelChildren()

        // close websocket connection with default status
        socket?.close(WEBSOCKET_CONNECTION_CLOSE_NORMAL_STATUS, null)
        socket?.cancel()

        // wait until websocket is fully closed
        try {
            withTimeout(REQUEST_TIMEOUT_MILLIS) {
                state.first { it == WebSocketConnectionStatus.DISCONNECTED }
            }
        } finally {
            state.value = WebSocketConnectionStatus.DISCONNECTED
        }

        // clear all the responses
        // we don't need them anymore
        responseMessages.resetReplayCache()
    }

    override suspend fun call(jsonRPCRequest: JsonRPCRequest): Result<JsonRPCResponse> {
        // connect only if connection wasn't already established
        if (state.value == WebSocketConnectionStatus.DISCONNECTED && !keepAlive.value) {
            connect()
        }

        // refresh timer on every call after which the websocket connection will be closed
        refreshDisconnectTimer()

        // send request async
        val json = jsonRequestAdapter.toJson(jsonRPCRequest)
        socket?.send(json) ?: return Result.failure(RuntimeException("No connection"))

        return coroutineScope {
            val response = async {
                withTimeout(REQUEST_TIMEOUT_MILLIS) {
                    responseMessages.first { it.id == jsonRPCRequest.id }
                }
            }

            requests.update { it + response }

            // wait for response
            val result = runCatching {
                response.await()
            }

            requests.update { it - response }

            result
        }
    }

    private fun close(ex: Throwable?) {
        // terminate all waiting requests
        requests.update { reqs ->
            reqs.forEach {
                it.cancel(CancellationException(message = ex?.message, cause = ex))
            }
            emptyList()
        }
        state.value = WebSocketConnectionStatus.DISCONNECTED
    }

    private inner class InnerWebSocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            state.value = WebSocketConnectionStatus.ESTABLISHED
            refreshDisconnectTimer()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val response = runCatching {
                jsonResponseAdapter.fromJson(text)
            }.getOrNull() ?: return

            refreshDisconnectTimer()
            responseMessages.tryEmit(response)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            close(RuntimeException(reason))
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            close(RuntimeException(reason))
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            close(t)
        }
    }
}