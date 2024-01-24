package com.tangem.blockchain.blockchains.bitcoincash

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.blockchain.blockchains.bitcoincash.network.BitconCashGetFeeResponse
import com.tangem.blockchain.blockchains.bitcoincash.network.SendTransactionRequest
import com.tangem.blockchain.extensions.AddHeaderInterceptor
import com.tangem.blockchain.network.BlockchainSdkRetrofitBuilder
import com.tangem.blockchain.network.blockbook.network.requests.GetFeeRequest
import com.tangem.blockchain.network.blockbook.network.responses.GetAddressResponse
import com.tangem.blockchain.network.blockbook.network.responses.GetUtxoResponseItem
import com.tangem.blockchain.network.blockbook.network.responses.SendTransactionResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import ru.gildor.coroutines.okhttp.await
import java.io.IOException

@OptIn(ExperimentalStdlibApi::class)
internal class BitcoinCashNowNodesApi(
    private val bchBookUrl: String,
    private val bchUrl: String,
    credentials: Pair<String, String>,
) {

    private val client = BlockchainSdkRetrofitBuilder.build(
        internalInterceptors = listOf(
            AddHeaderInterceptor(mapOf("Content-Type" to "application/json")),
            AddHeaderInterceptor(mapOf(credentials)),
        ),
    )

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    suspend fun getAddress(address: String): GetAddressResponse {
        return client
            .newCall(
                request = Request.Builder()
                    .get()
                    .url("${bchBookUrl}api/v2/address/$address?details=txs")
                    .build(),
            )
            .await()
            .unpack()
    }

    suspend fun getFee(): BitconCashGetFeeResponse {
        return client
            .newCall(
                request = Request.Builder()
                    .post(
                        moshi
                            .adapter<GetFeeRequest>()
                            .toJson(
                                GetFeeRequest.getFee(
                                    paramsList = emptyList(),
                                    method = "estimatefee",
                                ),
                            )
                            .toRequestBody(APPLICATION_JSON_MEDIA_TYPE.toMediaTypeOrNull()),
                    )
                    .url(bchUrl)
                    .build(),
            )
            .await()
            .unpack()
    }

    suspend fun sendTransaction(txHex: String): SendTransactionResponse {
        return client
            .newCall(
                request = Request.Builder()
                    .post(
                        moshi
                            .adapter<SendTransactionRequest>()
                            .toJson(SendTransactionRequest.getSendRequest(txHex))
                            .toRequestBody(APPLICATION_JSON_MEDIA_TYPE.toMediaTypeOrNull()),
                    )
                    .url(bchUrl)
                    .build(),
            )
            .await()
            .unpack()
    }

    suspend fun getUtxo(address: String): List<GetUtxoResponseItem> {
        return client
            .newCall(
                request = Request.Builder()
                    .get()
                    .url("$bchBookUrl/api/v2/utxo/$address")
                    .build(),
            )
            .await()
            .unpack<List<GetUtxoResponseItem>>()
            .filter {
                // filter unconfirmed UTXOs, to not block sending tx
                it.confirmations > 0
            }
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
    }
}