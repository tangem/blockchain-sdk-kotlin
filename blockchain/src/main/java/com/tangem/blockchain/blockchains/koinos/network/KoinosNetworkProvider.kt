package com.tangem.blockchain.blockchains.koinos.network

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.adapter
import com.tangem.blockchain.blockchains.koinos.KoinContractAbi
import com.tangem.blockchain.blockchains.koinos.models.KoinosTransactionEntry
import com.tangem.blockchain.blockchains.koinos.network.dto.KoinosChain
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
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.math.BigInteger

private interface KoinosRetrofitJsonRPCApi {
    @POST(" ")
    suspend fun send(@Header("apikey") apiKey: String? = null, @Body body: JsonRPCRequest): JsonRPCResponse

    @GET("v1/contract/koin/abi")
    suspend fun getConfig(@Header("apikey") apiKey: String? = null): KoinosContractResponse
}

/**
 * Koinos JSON-RPC interface
 * @see <a href="https://docs.koinos.io/rpc/json-rpc/">Koinos JSON-RPC docs</a>
 * @see <a href=https://github.com/koinos/koinos-proto/>Koinos api proto-models</a>
 */
internal class KoinosNetworkProvider(
    override val baseUrl: String,
    private val apiKey: String? = null,
    isTestnet: Boolean,
) : NetworkProvider {

    private val api = createRetrofitInstance(baseUrl).create(KoinosRetrofitJsonRPCApi::class.java)
    private val koinContractAbi = KoinContractAbi(isTestnet = isTestnet)

    suspend fun getKoinBalance(address: String, contractId: String): Result<Long> {
        val args = koinContractAbi.balanceOf.encodeArgs(address)
            ?: return decodeFailure(koinContractAbi.balanceOf.argsName)

        val request = KoinosMethod.ReadContract(
            contractId = contractId,
            entryPoint = koinContractAbi.balanceOf.entryPoint,
            args = args,
        ).asRequest()

        // Sometimes one request is not enough
        return retryCall {
            api.send(apiKey, request)
                .parseResult<KoinosMethod.ReadContract.Response>()
        }.map { response ->
            if (response.result == null) {
                0L
            } else {
                koinContractAbi.balanceOf.decodeResult(response.result)?.balance
                    ?: return decodeFailure(koinContractAbi.balanceOf.resultName)
            }
        }
    }

    suspend fun getContractId(): Result<String> {
        return catchNetworkError {
            val contractId = api.getConfig(apiKey).contractId
            Result.Success(contractId)
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

    suspend fun getResourceLimits(): Result<KoinosChain.ResourceLimitData> {
        val request = KoinosMethod.GetResourceLimits.asRequest()

        return catchNetworkError {
            api.send(apiKey, request).parseResult<KoinosMethod.GetResourceLimits.Response>()
        }.map {
            it.resourceLimitData
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
                sequenceNum = Long.MAX_VALUE,
                payerAddress = response.receipt.payer,
                maxPayerRC = response.receipt.maxPayerRc,
                rcLimit = response.receipt.rcLimit,
                rcUsed = response.receipt.rcUsed,
                event = KoinosTransactionEntry.Event.KoinTransferEvent(
                    fromAddress = KoinosTransactionEntry.Address.Single(decodedEvent.fromAccount),
                    toAddress = KoinosTransactionEntry.Address.Single(decodedEvent.toAccount),
                    value = decodedEvent.value,
                ),
            )
        }
    }

    suspend fun getTransactionHistory(request: TransactionHistoryRequest): Result<List<KoinosTransactionEntry>> {
        val seqNum = if (request.sequenceNum == 0L) null else request.sequenceNum.toString()

        val jsonRequest = KoinosMethod.GetAccountHistory(
            address = request.address,
            seqNumber = seqNum,
            limit = request.pageSize.toString(),
            ascending = false,
            irreversible = false,
        ).asRequest()

        return catchNetworkError {
            api.send(apiKey, jsonRequest).parseResult<KoinosMethod.GetAccountHistory.Response>()
        }.map { response ->
            response.values.mapNotNull {
                if (it.transaction != null) {
                    val receipt = it.transaction.receipt
                    val transferEvent = receipt.events.toTxEntryEvent()

                    KoinosTransactionEntry(
                        id = it.transaction.transaction.id,
                        sequenceNum = it.seqNum,
                        payerAddress = receipt.payer,
                        maxPayerRC = receipt.maxPayerRc,
                        rcLimit = receipt.rcLimit,
                        rcUsed = receipt.rcUsed,
                        event = transferEvent,
                    )
                } else {
                    null
                }
            }
        }
    }

    private fun List<KoinosProtocol.EventData>.toTxEntryEvent(): KoinosTransactionEntry.Event {
        // Filter all non koin transfer events
        val list = this.filter { it.name == koinContractAbi.transfer.eventName }
        return when {
            list.size == 1 -> {
                val eventData = list[0]
                koinContractAbi.transfer.decodeEvent(eventData.eventData)?.let { transfer ->
                    // the most likely case when we have one transfer event
                    KoinosTransactionEntry.Event.KoinTransferEvent(
                        fromAddress = KoinosTransactionEntry.Address.Single(transfer.fromAccount),
                        toAddress = KoinosTransactionEntry.Address.Single(transfer.toAccount),
                        value = transfer.value,
                    )
                } // if it's not transfer event
                    ?: KoinosTransactionEntry.Event.Unsupported
            }
            list.size > 1 -> {
                val decodedEvents = list.mapNotNull {
                    koinContractAbi.transfer.decodeEvent(it.eventData)
                }

                // if all events is not transfer
                if (decodedEvents.isEmpty()) {
                    return KoinosTransactionEntry.Event.Unsupported
                }

                val transfersMap = decodedEvents.groupBy { it.fromAccount }

                // if we found transfer event that came from the same address
                if (transfersMap.entries.size == 1) {
                    val entry = transfersMap.entries.first()
                    KoinosTransactionEntry.Event.KoinTransferEvent(
                        fromAddress = KoinosTransactionEntry.Address.Single(entry.key),
                        toAddress = KoinosTransactionEntry.Address.Multiple(entry.value.map { it.toAccount }),
                        value = entry.value.sumOf { it.value }, // sum up all transfers amounts
                    )
                } else { // multiple transfer events with different source addresses
                    val fromAddress = KoinosTransactionEntry.Address.Multiple(transfersMap.keys.toList())

                    // sum up all transfers amounts
                    val transfersSum = transfersMap.entries.sumOf { en -> en.value.sumOf { it.value } }
                    val destAddresses = transfersMap.entries
                        .map { en -> en.value.map { it.toAccount } }
                        .flatten()
                        .toSet()

                    val toAddress = if (destAddresses.size == 1) {
                        // if all transfers have one destination address
                        KoinosTransactionEntry.Address.Single(destAddresses.first())
                    } else {
                        KoinosTransactionEntry.Address.Multiple(destAddresses.toList())
                    }

                    KoinosTransactionEntry.Event.KoinTransferEvent(
                        fromAddress = fromAddress,
                        toAddress = toAddress,
                        value = transfersSum,
                    )
                }
            }
            else -> KoinosTransactionEntry.Event.Unsupported
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
                if (error.code == BlockchainSdkError.Koinos.InsufficientMana().code) {
                    BlockchainSdkError.Koinos.InsufficientMana()
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