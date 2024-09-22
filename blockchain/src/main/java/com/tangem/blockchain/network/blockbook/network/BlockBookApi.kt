package com.tangem.blockchain.network.blockbook.network

import com.squareup.moshi.adapter
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.JsonRPCRequest
import com.tangem.blockchain.common.JsonRPCResponse
import com.tangem.blockchain.common.logging.AddHeaderInterceptor
import com.tangem.blockchain.network.BlockchainSdkRetrofitBuilder
import com.tangem.blockchain.network.blockbook.config.BlockBookConfig
import com.tangem.blockchain.network.blockbook.config.BlockBookRequest
import com.tangem.blockchain.network.blockbook.network.responses.GetAddressResponse
import com.tangem.blockchain.network.blockbook.network.responses.GetFeeResponse
import com.tangem.blockchain.network.blockbook.network.responses.GetUtxoResponseItem
import com.tangem.blockchain.network.moshi
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import ru.gildor.coroutines.okhttp.await
import java.io.IOException

@OptIn(ExperimentalStdlibApi::class)
internal class BlockBookApi(private val config: BlockBookConfig, private val blockchain: Blockchain) {

    private val client = BlockchainSdkRetrofitBuilder.build(
        internalInterceptors = listOfNotNull(
            AddHeaderInterceptor(mapOf("Content-Type" to "application/json")),
            config.credentials?.let { AddHeaderInterceptor(mapOf(it)) },
        ),
    )

    suspend fun getAddress(address: String): GetAddressResponse {
        val requestBaseUrl = config.getRequestBaseUrl(BlockBookRequest.GetAddress(), blockchain)
        return client
            .newCall(
                request = Request.Builder()
                    .get()
                    .url("$requestBaseUrl/address/$address")
                    .build(),
            )
            .await()
            .unpack()
    }

    suspend fun getTransactions(
        address: String,
        page: String?,
        pageSize: Int,
        filterType: TransactionHistoryRequest.FilterType?,
    ): GetAddressResponse {
        val request = BlockBookRequest.GetAddress(page, pageSize, filterType)
        val requestBaseUrl = config.getRequestBaseUrl(request, blockchain)
        return client
            .newCall(
                request = Request.Builder()
                    .get()
                    .url("$requestBaseUrl/address/$address?details=txs${request.params()}")
                    .build(),
            )
            .await()
            .unpack()
    }

    suspend fun getTransaction(txId: String): GetAddressResponse.Transaction {
        val request = BlockBookRequest.GetTxById(txId)
        val requestBaseUrl = config.getRequestBaseUrl(request, blockchain)
        return client
            .newCall(
                request = Request.Builder()
                    .get()
                    .url("$requestBaseUrl/tx/${request.txId}")
                    .build(),
            )
            .await()
            .unpack()
    }

    suspend fun getFee(param: Int): GetFeeResponse {
        return client
            .newCall(
                request = Request.Builder()
                    .post(
                        moshi.adapter<JsonRPCRequest>()
                            .toJson(getFeeRequest(listOf(param)))
                            .toRequestBody(APPLICATION_JSON_MEDIA_TYPE.toMediaTypeOrNull()),
                    )
                    .url(config.getRequestBaseUrl(BlockBookRequest.GetFee, blockchain))
                    .build(),
            )
            .await()
            .unpack()
    }

    suspend fun sendTransaction(txHex: String): JsonRPCResponse {
        val requestBaseUrl = config.getRequestBaseUrl(BlockBookRequest.SendTransaction, blockchain)
        return client
            .newCall(
                request = Request.Builder()
                    .post(txHex.toRequestBody(TEXT_PLAIN_MEDIA_TYPE.toMediaTypeOrNull()))
                    .url("$requestBaseUrl/sendtx/")
                    .build(),
            )
            .await()
            .unpack()
    }

    suspend fun getUtxo(address: String): List<GetUtxoResponseItem> {
        val requestBaseUrl = config.getRequestBaseUrl(BlockBookRequest.GetUTXO, blockchain)
        return client
            .newCall(
                request = Request.Builder()
                    .get()
                    .url("$requestBaseUrl/utxo/$address")
                    .build(),
            )
            .await()
            .unpack<List<GetUtxoResponseItem>>()
            .filter {
                // filter unconfirmed UTXOs, to not block sending tx
                it.confirmations > 0
            }
    }

    private fun getFeeRequest(params: List<Int>): JsonRPCRequest {
        return JsonRPCRequest(method = "estimatesmartfee", params = params, id = "id")
    }

    private inline fun <reified T> Response.unpack(): T {
        val responseBody = body?.string()
        return if (isSuccessful && responseBody != null) {
            moshi.adapter<T>().fromJson(responseBody) ?: throw IOException("Response is null")
        } else {
            throw IOException("Response is null")
        }
    }

    private companion object {
        const val APPLICATION_JSON_MEDIA_TYPE = "application/json"
        const val TEXT_PLAIN_MEDIA_TYPE = "text/plain"
    }
}