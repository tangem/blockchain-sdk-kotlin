package com.tangem.blockchain.blockchains.bitcoincash

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.blockchain.blockchains.bitcoincash.apiservice.BitcoinCashApiService
import com.tangem.blockchain.blockchains.bitcoincash.apiservice.BitcoinCashBlockBookApiService
import com.tangem.blockchain.blockchains.bitcoincash.network.BitconCashGetFeeResponse
import com.tangem.blockchain.blockchains.bitcoincash.network.SendTransactionRequest
import com.tangem.blockchain.extensions.AddHeaderInterceptor
import com.tangem.blockchain.network.BlockchainSdkRetrofitBuilder
import com.tangem.blockchain.network.blockbook.network.requests.GetFeeRequest
import com.tangem.blockchain.network.blockbook.network.responses.GetAddressResponse
import com.tangem.blockchain.network.blockbook.network.responses.GetUtxoResponseItem
import com.tangem.blockchain.network.blockbook.network.responses.SendTransactionResponse
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import okhttp3.OkHttpClient
import retrofit2.Response
import java.io.IOException

internal class BitcoinCashNowNodesApi(
    bchBookUrl: String,
    bchUrl: String,
    credentials: Pair<String, String>,
) {
    private val client: OkHttpClient = BlockchainSdkRetrofitBuilder.build(
        internalInterceptors = listOf(
            AddHeaderInterceptor(mapOf("Content-Type" to "application/json")),
            AddHeaderInterceptor(mapOf(credentials)),
        ),
    )

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val retrofitBlockBook: Retrofit = Retrofit.Builder()
        .client(client)
        .baseUrl(bchBookUrl)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val retrofitBch: Retrofit = Retrofit.Builder()
        .client(client)
        .baseUrl(bchUrl)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val blockBookService: BitcoinCashBlockBookApiService =
        retrofitBlockBook.create(BitcoinCashBlockBookApiService::class.java)
    private val bchService: BitcoinCashApiService = retrofitBch.create(BitcoinCashApiService::class.java)

    suspend fun getAddress(address: String): GetAddressResponse {
        val response = blockBookService.getAddress(address)
        return response.unpack()
    }

    suspend fun getFee(): BitconCashGetFeeResponse {
        val request = GetFeeRequest.getFee(
            paramsList = emptyList(),
            method = "estimatefee",
        )
        val response = bchService.getFee(request)
        return response.unpack()
    }

    suspend fun sendTransaction(txHex: String): SendTransactionResponse {
        val request = SendTransactionRequest.getSendRequest(txHex)
        val response = bchService.sendTransaction(request)
        return response.unpack()
    }

    suspend fun getUtxo(address: String): List<GetUtxoResponseItem> {
        val response = blockBookService.getUtxo(address)
        return response.unpack<List<GetUtxoResponseItem>>().filter {
            it.confirmations > 0  // filter unconfirmed UTXOs, to not block sending tx
        }
    }

    private inline fun <reified T> Response<T>.unpack(): T {
        val responseBody = body()
        return if (isSuccessful && responseBody != null) {
            responseBody
        } else {
            throw IOException("Error: ${errorBody()?.string()}")
        }
    }
}