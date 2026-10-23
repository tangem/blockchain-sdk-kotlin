package com.tangem.blockchain.blockchains.sui.network.grpc

import com.squareup.wire.GrpcClient
import com.squareup.wire.GrpcException
import com.squareup.wire.GrpcStatus
import com.tangem.blockchain.blockchains.sui.model.SuiCoin
import com.tangem.blockchain.blockchains.sui.model.SuiDryRunResult
import com.tangem.blockchain.blockchains.sui.model.SuiExecuteResult
import com.tangem.blockchain.blockchains.sui.network.SuiConstants
import com.tangem.blockchain.blockchains.sui.network.grpc.proto.Bcs
import com.tangem.blockchain.blockchains.sui.network.grpc.proto.ExecuteTransactionRequest
import com.tangem.blockchain.blockchains.sui.network.grpc.proto.FieldMask
import com.tangem.blockchain.blockchains.sui.network.grpc.proto.GetEpochRequest
import com.tangem.blockchain.blockchains.sui.network.grpc.proto.GetTransactionRequest
import com.tangem.blockchain.blockchains.sui.network.grpc.proto.GrpcLedgerServiceClient
import com.tangem.blockchain.blockchains.sui.network.grpc.proto.GrpcStateServiceClient
import com.tangem.blockchain.blockchains.sui.network.grpc.proto.GrpcTransactionExecutionServiceClient
import com.tangem.blockchain.blockchains.sui.network.grpc.proto.ListOwnedObjectsRequest
import com.tangem.blockchain.blockchains.sui.network.grpc.proto.Object
import com.tangem.blockchain.blockchains.sui.network.grpc.proto.SimulateTransactionRequest
import com.tangem.blockchain.blockchains.sui.network.grpc.proto.Transaction
import com.tangem.blockchain.blockchains.sui.network.grpc.proto.UserSignature
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.BlockchainSdkRetrofitBuilder
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import java.math.BigDecimal

/**
 * Sui full node client over gRPC API v2, which replaced the JSON-RPC API shut down by Sui.
 *
 * [headers] carry the provider credentials: gRPC endpoints take API keys as metadata, not in the URL.
 */
internal class SuiGrpcProvider(
    override val baseUrl: String,
    private val headers: Map<String, String> = emptyMap(),
) : NetworkProvider {

    private val grpcClient: GrpcClient by lazy {
        GrpcClient.Builder()
            .client(createOkHttpClient())
            .baseUrl(baseUrl)
            // Sui nodes reject gzip-compressed requests with UNIMPLEMENTED, and Wire compresses every message by default
            .minMessageToCompress(Long.MAX_VALUE)
            .build()
    }

    private val ledgerService by lazy { GrpcLedgerServiceClient(grpcClient) }
    private val stateService by lazy { GrpcStateServiceClient(grpcClient) }
    private val executionService by lazy { GrpcTransactionExecutionServiceClient(grpcClient) }

    suspend fun getReferenceGasPrice(): Result<BigDecimal> = grpcCall {
        val request = GetEpochRequest(read_mask = FieldMask(paths = listOf("reference_gas_price")))
        val response = ledgerService.GetEpoch().execute(request)

        response.epoch?.reference_gas_price?.toUnsignedBigDecimal()
            ?: missingField("epoch.reference_gas_price")
    }

    suspend fun getCoins(address: String): Result<List<SuiCoin>> = grpcCall {
        val coins = mutableListOf<SuiCoin>()
        var pageToken: ByteString? = null
        do {
            val request = ListOwnedObjectsRequest(
                owner = address,
                page_size = MAX_PAGE_SIZE,
                page_token = pageToken,
                read_mask = FieldMask(paths = listOf("object_id", "version", "digest", "object_type", "balance")),
                object_type = COIN_OBJECT_TYPE,
            )
            val response = stateService.ListOwnedObjects().execute(request)
            response.objects.mapTo(coins) { it.toSuiCoin() }
            pageToken = response.next_page_token?.takeIf { it.size > 0 }
        } while (pageToken != null)

        coins
    }

    suspend fun dryRunTransaction(transactionBytes: String): Result<SuiDryRunResult> = grpcCall {
        val request = SimulateTransactionRequest(
            transaction = Transaction(bcs = Bcs(value_ = transactionBytes.decodeBase64OrThrow())),
            read_mask = FieldMask(
                paths = listOf(
                    "transaction.effects.status",
                    "transaction.effects.gas_used",
                    "transaction.transaction.gas_payment.price",
                ),
            ),
        )
        val executed = executionService.SimulateTransaction().execute(request).transaction
            ?: missingField("transaction")
        val effects = executed.effects ?: missingField("transaction.effects")
        val gasUsed = effects.gas_used ?: missingField("transaction.effects.gas_used")

        SuiDryRunResult(
            isSuccess = effects.status?.success == true,
            computationCost = gasUsed.computation_cost?.toUnsignedBigDecimal() ?: BigDecimal.ZERO,
            storageCost = gasUsed.storage_cost?.toUnsignedBigDecimal() ?: BigDecimal.ZERO,
            gasPrice = executed.transaction?.gas_payment?.price
                ?: missingField("transaction.transaction.gas_payment.price"),
        )
    }

    suspend fun executeTransaction(transactionBytes: String, signature: String): Result<SuiExecuteResult> = grpcCall {
        val request = ExecuteTransactionRequest(
            transaction = Transaction(bcs = Bcs(value_ = transactionBytes.decodeBase64OrThrow())),
            signatures = listOf(UserSignature(bcs = Bcs(value_ = signature.decodeBase64OrThrow()))),
            read_mask = FieldMask(paths = listOf("digest", "effects.status")),
        )
        val executed = executionService.ExecuteTransaction().execute(request).transaction
            ?: missingField("transaction")

        SuiExecuteResult(
            digest = executed.digest ?: missingField("transaction.digest"),
            isSuccess = executed.effects?.status?.success == true,
        )
    }

    /** Fails with [BlockchainSdkError.Sui.Api] carrying [GrpcStatus.NOT_FOUND] while the transaction is unknown */
    suspend fun getTransaction(digest: String): Result<String> = grpcCall {
        val request = GetTransactionRequest(digest = digest, read_mask = FieldMask(paths = listOf("digest")))

        ledgerService.GetTransaction().execute(request).transaction?.digest ?: missingField("transaction.digest")
    }

    private fun createOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
        if (headers.isNotEmpty()) {
            builder.addInterceptor { chain ->
                val request = chain.request().newBuilder()
                headers.forEach { (name, value) -> request.header(name, value) }
                chain.proceed(request.build())
            }
        }
        BlockchainSdkRetrofitBuilder.timeoutConfig?.let { config ->
            builder.callTimeout(config.call.time, config.call.unit)
            builder.connectTimeout(config.connect.time, config.connect.unit)
            builder.readTimeout(config.read.time, config.read.unit)
            builder.writeTimeout(config.write.time, config.write.unit)
        }
        return builder.build()
    }

    private fun Object.toSuiCoin(): SuiCoin {
        val objectType = object_type ?: missingField("object.object_type")

        return SuiCoin(
            objectId = object_id ?: missingField("object.object_id"),
            coinType = SuiConstants.coinTypeOf(coinObjectType = objectType),
            version = version ?: missingField("object.version"),
            digest = digest ?: missingField("object.digest"),
            mistBalance = balance?.toUnsignedBigDecimal() ?: missingField("object.balance"),
        )
    }

    private fun String.decodeBase64OrThrow(): ByteString =
        decodeBase64() ?: throw IllegalArgumentException("Transaction data is not a valid base64 string")

    private fun missingField(path: String): Nothing = error("Sui gRPC response misses $path")

    /** uint64 fields arrive as a signed Long, so balances above Long.MAX_VALUE would turn negative */
    private fun Long.toUnsignedBigDecimal(): BigDecimal = BigDecimal(toULong().toString())

    private suspend inline fun <T> grpcCall(crossinline block: suspend () -> T): Result<T> = try {
        Result.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: GrpcException) {
        Result.Failure(e.toSuiError())
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        Result.Failure(e.toBlockchainSdkError())
    }

    /**
     * gRPC answers HTTP 200 and reports failures in the `grpc-status` trailer. [GrpcException] is an IOException,
     * which `MultiNetworkProvider` treats as a reason to switch the provider, so only provider-side statuses are
     * kept as such; request-level ones (e.g. NOT_FOUND for an unconfirmed transaction) become [BlockchainSdkError.Sui.Api].
     */
    private fun GrpcException.toSuiError(): BlockchainSdkError = if (grpcStatus in PROVIDER_FAILURE_STATUSES) {
        toBlockchainSdkError()
    } else {
        BlockchainSdkError.Sui.Api(code = grpcStatus.code, message = grpcMessage ?: grpcStatus.name)
    }

    private companion object {
        const val MAX_PAGE_SIZE = 1000
        const val COIN_OBJECT_TYPE = "0x2::coin::Coin"

        val PROVIDER_FAILURE_STATUSES = setOf(
            GrpcStatus.CANCELLED,
            GrpcStatus.UNKNOWN,
            GrpcStatus.DEADLINE_EXCEEDED,
            GrpcStatus.PERMISSION_DENIED,
            GrpcStatus.RESOURCE_EXHAUSTED,
            GrpcStatus.UNIMPLEMENTED,
            GrpcStatus.INTERNAL,
            GrpcStatus.UNAVAILABLE,
            GrpcStatus.UNAUTHENTICATED,
        )
    }
}