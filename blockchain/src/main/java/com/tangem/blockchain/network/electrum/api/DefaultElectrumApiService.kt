package com.tangem.blockchain.network.electrum.api

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.adapter
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.JsonRPCRequest
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.fold
import com.tangem.blockchain.extensions.map
import com.tangem.blockchain.network.jsonrpc.JsonRPCService
import com.tangem.blockchain.network.moshi
import java.math.BigDecimal

internal class DefaultElectrumApiService(
    val rpcService: JsonRPCService,
) : ElectrumApiService {

    private val blockTipAdapter: JsonAdapter<ElectrumResponse.BlockTip> by lazy {
        moshi.adapter(ElectrumResponse.BlockTip::class.java)
    }

    private val balanceAdapter: JsonAdapter<ElectrumResponse.Balance> by lazy {
        moshi.adapter(ElectrumResponse.Balance::class.java)
    }

    private val getTransactionAdapter: JsonAdapter<ElectrumResponse.Transaction> by lazy {
        moshi.adapter(ElectrumResponse.Transaction::class.java)
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

    override suspend fun getBalance(addressScriptHash: String): Result<ElectrumResponse.Balance> {
        return requestNotNull(
            method = "blockchain.scripthash.get_balance",
            params = listOf(addressScriptHash),
            adapter = balanceAdapter,
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun getTransactionHistory(
        addressScriptHash: String,
    ): Result<List<ElectrumResponse.TxHistoryEntry>> {
        return requestNotNull(
            method = "blockchain.scripthash.get_history",
            params = listOf(addressScriptHash),
            adapter = moshi.adapter<List<ElectrumResponse.TxHistoryEntry>>(),
        )
    }

    override suspend fun getTransaction(txHash: String): Result<ElectrumResponse.Transaction> {
        return requestNotNull(
            method = "blockchain.transaction.get",
            params = listOf(txHash, true),
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
    override suspend fun getUnspentUTXOs(addressScriptHash: String): Result<List<ElectrumResponse.UnspentUTXORecord>> {
        return requestNotNull(
            method = "blockchain.scripthash.listunspent",
            params = listOf(addressScriptHash),
            adapter = moshi.adapter<List<ElectrumResponse.UnspentUTXORecord>>(),
        )
    }

    override suspend fun getEstimateFee(numberConfirmationBlocks: Int): Result<ElectrumResponse.EstimateFee> {
        return requestNotNull<BigDecimal>(
            method = "blockchain.estimatefee",
            params = listOf(numberConfirmationBlocks),
        ).map {
            // If the daemon does not have enough information to make an estimate, the integer -1 is returned.
            ElectrumResponse.EstimateFee(
                feeInCoinsPer1000Bytes = it.takeIf { it != BigDecimal.ONE.unaryMinus() },
            )
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private suspend inline fun <reified T> requestNotNull(
        method: String,
        params: List<Any> = emptyList(),
        adapter: JsonAdapter<T> = moshi.adapter<T>(),
    ): Result<T> {
        return rpcService.call(
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