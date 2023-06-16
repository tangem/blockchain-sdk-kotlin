package com.tangem.blockchain.blockchains.stellar

import com.tangem.blockchain.common.NetworkProvider
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.stellar.sdk.Server
import org.stellar.sdk.Transaction
import org.stellar.sdk.requests.RequestBuilder
import org.stellar.sdk.responses.AccountResponse
import org.stellar.sdk.responses.FeeStatsResponse
import org.stellar.sdk.responses.LedgerResponse
import org.stellar.sdk.responses.Page
import org.stellar.sdk.responses.RootResponse
import org.stellar.sdk.responses.SubmitTransactionResponse
import org.stellar.sdk.responses.operations.OperationResponse
import shadow.okhttp3.OkHttpClient
import java.io.IOException

internal class StellarWrapperNetworkProvider(
    private val server: Server,
    url: String,
) : NetworkProvider {
    override val baseUrl: String = url.toHttpUrlOrNull()?.host ?: ""

    val httpClient: OkHttpClient
        get() = server.httpClient

    @Throws(IOException::class)
    fun submitTransaction(transaction: Transaction?): SubmitTransactionResponse {
        return server.submitTransaction(transaction)
    }

    fun accountCall(data: String): AccountResponse {
        return server.accounts().account(data)
    }

    fun rootCall(): RootResponse {
        return server.root()
    }

    fun ledgerCall(ledgerSeq: Long): LedgerResponse {
        return server.ledgers().ledger(ledgerSeq)
    }

    fun paymentsCall(accountId: String): Page<OperationResponse> {
        return server.payments().forAccount(accountId).order(RequestBuilder.Order.DESC).execute()
    }

    fun feeCall(): FeeStatsResponse {
        return server.feeStats().execute()
    }

    fun operationsLimit(accountId: String): Page<OperationResponse> {
        return server.operations().forAccount(accountId)
            .limit(RECORD_LIMIT)
            .includeFailed(true)
            .execute()
    }
}