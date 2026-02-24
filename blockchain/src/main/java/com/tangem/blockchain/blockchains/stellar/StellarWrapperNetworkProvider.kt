package com.tangem.blockchain.blockchains.stellar

import com.tangem.blockchain.blockchains.stellar.StellarNetworkService.Companion.HTTP_NOT_FOUND_CODE
import com.tangem.blockchain.common.NetworkProvider
import com.tangem.blockchain.common.toBlockchainSdkError
import com.tangem.blockchain.extensions.Result
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import org.stellar.sdk.Server
import org.stellar.sdk.Transaction
import org.stellar.sdk.exception.NetworkException
import org.stellar.sdk.requests.RequestBuilder
import org.stellar.sdk.responses.*
import org.stellar.sdk.responses.operations.OperationResponse
import java.io.IOException

internal class StellarWrapperNetworkProvider(
    private val server: Server,
    url: String,
) : NetworkProvider {
    override val baseUrl: String = url.toHttpUrlOrNull()?.host ?: ""

    val httpClient: OkHttpClient
        get() = server.httpClient

    @Throws(IOException::class)
    fun submitTransaction(transaction: Transaction): Result<TransactionResponse> {
        return runWithErrorHandling { server.submitTransaction(transaction) }
    }

    fun accountCall(data: String): Result<AccountResponse> {
        return runWithErrorHandling { server.accounts().account(data) }
    }

    fun rootCall(): Result<RootResponse> {
        return runWithErrorHandling { server.root().execute() }
    }

    fun ledgerCall(ledgerSeq: Long): Result<LedgerResponse> {
        return runWithErrorHandling { server.ledgers().ledger(ledgerSeq) }
    }

    fun paymentsCall(accountId: String): Result<List<OperationResponse>> {
        return runWithErrorHandling(throwExceptionWhenNotFound = false) {
            server
                .payments()
                .forAccount(accountId)
                .order(RequestBuilder.Order.DESC)
                .execute()
                .records
        }
    }

    fun feeCall(): Result<FeeStatsResponse> {
        return runWithErrorHandling { server.feeStats().execute() }
    }

    fun operationsLimit(accountId: String): Result<Page<OperationResponse>> {
        return runWithErrorHandling {
            server.operations().forAccount(accountId)
                .limit(RECORD_LIMIT)
                .includeFailed(true)
                .execute()
        }
    }

    private fun <T> runWithErrorHandling(throwExceptionWhenNotFound: Boolean = true, block: () -> T): Result<T> {
        return try {
            val result = block()
            Result.Success(result)
        } catch (exception: Exception) {
            if (exception is NetworkException && exception.code == HTTP_NOT_FOUND_CODE && throwExceptionWhenNotFound) {
                throw exception // handled in NetworkService
            } else {
                Result.Failure(exception.toBlockchainSdkError())
            }
        }
    }
}