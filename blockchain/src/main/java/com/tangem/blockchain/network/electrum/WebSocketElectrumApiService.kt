package com.tangem.blockchain.network.electrum

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.adapter
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.JsonRPCRequest
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.fold
import com.tangem.blockchain.extensions.map
import com.tangem.blockchain.network.jsonrpc.DefaultJsonRPCWebsocketService
import com.tangem.blockchain.network.moshi
import okhttp3.OkHttpClient

internal class WebSocketElectrumApiService(
    wssUrl: String,
    okHttpClient: OkHttpClient,
) : ElectrumApiService {

    private val service = DefaultJsonRPCWebsocketService(
        wssUrl = wssUrl,
        pingPongRequestFactory = {
            JsonRPCRequest(
                method = "server.ping",
                id = "keepAlive",
                params = emptyList<String>(),
            )
        },
        okHttpClient = okHttpClient,
    )

    private val blockTipAdapter: JsonAdapter<ElectrumResponse.BlockTip> by lazy {
        ElectrumResponse_BlockTipJsonAdapter(moshi)
    }

    private val balanceAdapter: JsonAdapter<ElectrumResponse.Balance> by lazy {
        ElectrumResponse_BalanceJsonAdapter(moshi)
    }

    private val getTransactionAdapter: JsonAdapter<ElectrumResponse.Transaction> by lazy {
        ElectrumResponse_TransactionJsonAdapter(moshi)
    }

    override suspend fun getServerVersion(
        clientName: String,
        supportedProtocolVersion: String,
    ): Result<ElectrumResponse.ServerInfo> {
        val response = requestNotNull<List<String>>(
            method = "server.version",
            params = listOf(clientName, supportedProtocolVersion),
        )

        return response.fold(
            success = { list ->
                if (list.count() < 2) {
                    Result.Failure(
                        BlockchainSdkError.UnsupportedOperation(
                            "Unknown JSON-RPC response result data",
                        ),
                    )
                } else {
                    Result.Success(
                        ElectrumResponse.ServerInfo(
                            applicationName = list[0].ifEmpty { "Undefined" },
                            versionNumber = list[1],
                        ),
                    )
                }
            },
            failure = {
                Result.Failure(it)
            },
        )
    }

    override suspend fun getBlockTip(): Result<ElectrumResponse.BlockTip> {
        return requestNotNull(
            method = "blockchain.headers.tip",
            adapter = blockTipAdapter,
        )
    }

    override suspend fun getBalance(address: String): Result<ElectrumResponse.Balance> {
        return requestNotNull(
            method = "blockchain.address.get_balance",
            params = listOf(address, "exclude_tokens"),
            adapter = balanceAdapter,
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun getTransactionHistory(address: String): Result<List<ElectrumResponse.TxHistoryEntry>> {
        return requestNotNull(
            method = "blockchain.address.get_history",
            params = listOf(address),
            adapter = moshi.adapter<List<ElectrumResponse.TxHistoryEntry>>(),
        )
    }
    override suspend fun getTransaction(address: String): Result<ElectrumResponse.Transaction> {
        return requestNotNull(
            method = "blockchain.transaction.get",
            params = listOf(address, true),
            adapter = getTransactionAdapter,
        )
    }

    override suspend fun sendTransaction(rawTransactionHex: String): Result<ElectrumResponse.TxHex> {
        return requestNotNull<String>(
            method = "blockchain.transaction.broadcast",
            params = listOf(rawTransactionHex),
        ).map {
            ElectrumResponse.TxHex(it)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private suspend inline fun <reified T> requestNotNull(
        method: String,
        params: List<Any> = emptyList(),
        adapter: JsonAdapter<T> = moshi.adapter<T>(),
    ): Result<T> {
        return service.call(
            JsonRPCRequest(
                method = method,
                params = params,
            ),
        ).fold(
            onSuccess = { response ->
                when {
                    response.error != null -> Result.Failure(
                        BlockchainSdkError.ElectrumBlockchain.Api(
                            code = response.error.code,
                            message = response.error.message,
                        ),
                    )
                    else -> {
                        runCatching {
                            adapter.fromJsonValue(response.result)
                        }.getOrNull()?.let { Result.Success(it) } ?: Result.Failure(
                            BlockchainSdkError.UnsupportedOperation(
                                "Unknown Electrum JSON-RPC response result",
                            ),
                        )
                    }
                }
            },
            onFailure = {
                Result.Failure(BlockchainSdkError.WrappedThrowable(it))
            },
        )
    }
}