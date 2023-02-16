package com.tangem.blockchain.network.blockbook.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.addHeaders
import com.tangem.blockchain.network.blockbook.config.BlockBookConfig
import com.tangem.blockchain.network.blockbook.config.BlockBookRequest
import com.tangem.blockchain.network.blockbook.network.requests.GetFeeRequest
import com.tangem.blockchain.network.blockbook.network.responses.GetAddressResponse
import com.tangem.blockchain.network.blockbook.network.responses.GetFeeResponse
import com.tangem.blockchain.network.blockbook.network.responses.GetUtxoResponseItem
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import ru.gildor.coroutines.okhttp.await

@OptIn(ExperimentalStdlibApi::class)
internal class BlockBookApi(
    private val config: BlockBookConfig,
    private val blockchain: Blockchain
) {

    private val client = OkHttpClient.Builder()
        .addHeaders(headers = mapOf(config.credentials))
        .build()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    suspend fun getAddress(address: String): GetAddressResponse {
        val requestBaseUrl = config.getRequestBaseUrl(BlockBookRequest.GET_ADDRESS, blockchain)
        return client
            .newCall(
                request = Request.Builder()
                    .get()
                    .url("$requestBaseUrl/address/$address?details=txs")
                    .build()
            )
            .await()
            .unpack()
    }

    suspend fun getFee(): GetFeeResponse {
        return client
            .newCall(
                request = Request.Builder()
                    .post(moshi.adapter<GetFeeRequest>().toJson(GetFeeRequest.FEE).toRequestBody())
                    .url(config.getRequestBaseUrl(BlockBookRequest.GET_FEE, blockchain))
                    .build()
            )
            .await()
            .unpack()
    }

    suspend fun sendTransaction(txHex: String) {
        val requestBaseUrl = config.getRequestBaseUrl(BlockBookRequest.SEND_TRANSACTION, blockchain)
        val response = client
            .newCall(
                request = Request.Builder()
                    .get()
                    .url("$requestBaseUrl/sendtx/$txHex")
                    .build()
            )
            .await()

        val responseBody = response.body?.string()
        if (response.isSuccessful && responseBody != null) {
            return
        } else {
            throw IllegalStateException("Response is null")
        }
    }

    suspend fun getUtxo(address: String): List<GetUtxoResponseItem> {
        val requestBaseUrl = config.getRequestBaseUrl(BlockBookRequest.GET_UTXO, blockchain)
        return client
            .newCall(
                request = Request.Builder()
                    .get()
                    .url("$requestBaseUrl/utxo/$address")
                    .build()
            )
            .await()
            .unpack()
    }

    private inline fun <reified T> Response.unpack(): T {
        val responseBody = body?.string()
        return if (isSuccessful && responseBody != null) {
            moshi.adapter<T>().fromJson(responseBody)
                ?: throw IllegalStateException("Response is null")
        } else {
            throw IllegalStateException("Response is null")
        }
    }
}