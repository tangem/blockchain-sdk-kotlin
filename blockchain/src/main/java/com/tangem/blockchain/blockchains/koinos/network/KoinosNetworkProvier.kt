package com.tangem.blockchain.blockchains.koinos.network

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.adapter
import com.tangem.blockchain.blockchains.koinos.KoinContractAbi
import com.tangem.blockchain.blockchains.koinos.models.KoinosTransactionEntry
import com.tangem.blockchain.blockchains.koinos.network.dto.KoinosMethod
import com.tangem.blockchain.blockchains.koinos.network.dto.KoinosProtocol
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.JsonRPCRequest
import com.tangem.blockchain.common.JsonRPCResponse
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.map
import com.tangem.blockchain.network.createRetrofitInstance
import com.tangem.blockchain.network.moshi
import kotlinx.coroutines.delay
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.math.BigInteger

private interface KoinosRetrofitJsonRPCApi {
    @POST("/")
    suspend fun send(@Header("apikey") apiKey: String? = null, @Body body: JsonRPCRequest): JsonRPCResponse
}

/**
 * Koinos JSON-RPC interface
 * @see <a href="https://docs.koinos.io/rpc/json-rpc/">Koinos JSON-RPC docs</a>
 * @see <a href=https://github.com/koinos/koinos-proto/>Koinos api proto-models</a>
 */
internal class KoinosNetworkProvier(
    override val baseUrl: String,
    isTestnet: Boolean,
    private val apiKey: String? = null,
) : NetworkProvider {

    private val api = createRetrofitInstance(baseUrl).create(KoinosRetrofitJsonRPCApi::class.java)
    private val koinContractAbi = KoinContractAbi(isTestnet = isTestnet)

    suspend fun getKoinBalance(address: String): Result<Long> {
        val args = koinContractAbi.balanceOf.encodeArgs(address)
            ?: return decodeFailure(koinContractAbi.balanceOf.argsName)

        val request = KoinosMethod.ReadContract(
            contractId = koinContractAbi.contractId,
            entryPoint = koinContractAbi.balanceOf.entryPoint,
            args = args,
        ).asRequest()

        // Sometimes one request is not enough
        return retryCall {
            api.send(apiKey, request).parseResult<KoinosMethod.ReadContract.Response>()
        }.map { response ->
            response.result?.let {
                koinContractAbi.balanceOf.decodeResult(it)?.balance
            } ?: return decodeFailure(koinContractAbi.balanceOf.resultName)
        }
    }

    suspend fun getRC(address: String): Result<Long> {
        val request = KoinosMethod.GetAccountRC(
            account = address,
        ).asRequest()

        return catchNetworkError {
            api.send(apiKey, request).parseResult<KoinosMethod.GetAccountRC.Response>()
        }.map { it.rc }
    }

    suspend fun getNonce(address: String): Result<BigInteger> {
        val request = KoinosMethod.GetAccountNonce(
            account = address,
        ).asRequest()

        return catchNetworkError {
            api.send(apiKey, request).parseResult<KoinosMethod.GetAccountNonce.Response>()
        }.map {
            it.decode() ?: return decodeFailure(it.nonceTypeName)
        }
    }

    suspend fun submitTransaction(transaction: KoinosProtocol.Transaction): Result<KoinosTransactionEntry> {
        val jsonRequest = KoinosMethod.SubmitTransaction(
            transaction = transaction,
            broadcast = true,
        ).asRequest()

        return catchNetworkError {
            api.send(apiKey, jsonRequest).parseResult<KoinosMethod.SubmitTransaction.Response>()
        }.map { response ->
            val encodedEvent = response.receipt.events.getOrNull(0)?.eventData
                ?: return decodeFailure(koinContractAbi.transfer.eventName)
            val decodedEvent = koinContractAbi.transfer.decodeEvent(encodedEvent)
                ?: return decodeFailure(koinContractAbi.transfer.eventName)

            KoinosTransactionEntry(
                id = response.receipt.id,
                payerAddress = response.receipt.payer,
                maxPayerRC = response.receipt.maxPayerRc,
                rcLimit = response.receipt.rcLimit,
                rcUsed = response.receipt.rcUsed,
                transferEvent = KoinosTransactionEntry.KoinTransferEvent(
                    fromAddress = decodedEvent.fromAccount,
                    toAddress = decodedEvent.toAccount,
                    value = decodedEvent.value,
                ),
            )
        }
    }

    private inline fun <T> catchNetworkError(block: () -> Result<T>): Result<T> {
        return runCatching {
            block()
        }.getOrElse {
            Result.Failure(BlockchainSdkError.WrappedThrowable(it))
        }
    }

    private suspend fun <T> retryCall(
        times: Int = 3,
        initialDelay: Long = 100,
        maxDelay: Long = 1000,
        factor: Double = 2.0,
        block: suspend () -> Result<T>,
    ): Result<T> {
        var currentDelay = initialDelay
        repeat(times - 1) {
            val res = catchNetworkError {
                block()
            }

            if (res is Result.Failure && res.error is BlockchainSdkError.Koinos.Api) {
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            } else {
                return res
            }
        }
        return block() // last attempt
    }

    private fun decodeFailure(decoderName: String) =
        Result.Failure(BlockchainSdkError.Koinos.ProtobufDecodeError(protoType = decoderName))

    @OptIn(ExperimentalStdlibApi::class)
    private inline fun <reified T> JsonRPCResponse.parseResult(
        adapter: JsonAdapter<T> = moshi.adapter<T>(),
    ): Result<T> {
        return if (error != null) {
            Result.Failure(
                if (error.code == BlockchainSdkError.Koinos.InsufficientMana.code) {
                    BlockchainSdkError.Koinos.InsufficientMana
                } else {
                    BlockchainSdkError.Koinos.Api(
                        code = error.code,
                        message = error.message,
                    )
                },
            )
        } else {
            runCatching {
                adapter.fromJsonValue(result)
            }.onFailure {
                Log.e("KoinosApi", it.stackTraceToString())
            }.getOrNull()?.let { Result.Success(it) } ?: Result.Failure(
                BlockchainSdkError.UnsupportedOperation(
                    "Unknown Koinos JSON-RPC response result",
                ),
            )
        }
    }
}