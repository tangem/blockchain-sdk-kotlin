package com.tangem.blockchain.blockchains.bitcoincash

import com.tangem.blockchain.blockchains.bitcoincash.api.BitcoinCashApi
import com.tangem.blockchain.blockchains.bitcoincash.api.BitcoinCashBlockBookApi
import com.tangem.blockchain.blockchains.bitcoincash.network.BitconCashGetFeeResponse
import com.tangem.blockchain.common.JsonRPCRequest
import com.tangem.blockchain.common.logging.AddHeaderInterceptor
import com.tangem.blockchain.network.blockbook.network.responses.GetAddressResponse
import com.tangem.blockchain.network.blockbook.network.responses.GetUtxoResponseItem
import com.tangem.blockchain.network.blockbook.network.responses.SendTransactionResponse
import com.tangem.blockchain.network.createRetrofitInstance
import retrofit2.Response
import java.io.IOException

internal class BitcoinCashNowNodesApiService(
    bchBookUrl: String,
    bchUrl: String,
    credentials: Pair<String, String>,
) {
    private val interceptors = listOf(
        AddHeaderInterceptor(mapOf("Content-Type" to "application/json")),
        AddHeaderInterceptor(mapOf(credentials)),
    )

    private val retrofitBlockBook = createRetrofitInstance(bchBookUrl, interceptors)
    private val retrofitBch = createRetrofitInstance(bchUrl, interceptors)

    private val blockBookService: BitcoinCashBlockBookApi =
        retrofitBlockBook.create(BitcoinCashBlockBookApi::class.java)
    private val bchService: BitcoinCashApi = retrofitBch.create(BitcoinCashApi::class.java)

    suspend fun getAddress(address: String): GetAddressResponse {
        val response = blockBookService.getAddress(address)
        return response.unpack()
    }

    suspend fun getFee(): BitconCashGetFeeResponse {
        val response = bchService.getFee(request = getFeeRequest())
        return response.unpack()
    }

    suspend fun sendTransaction(txHex: String): SendTransactionResponse {
        val response = bchService.sendTransaction(request = getSendRequest(txHex))
        return response.unpack()
    }

    suspend fun getUtxo(address: String): List<GetUtxoResponseItem> {
        val response = blockBookService.getUtxo(address)
        return response.unpack<List<GetUtxoResponseItem>>().filter {
            it.confirmations > 0 // filter unconfirmed UTXOs, to not block sending tx
        }
    }

    private fun getFeeRequest(): JsonRPCRequest {
        return JsonRPCRequest(
            method = "estimatefee",
            params = emptyList<Int>(),
            id = "id",
        )
    }

    private fun getSendRequest(signedTransaction: String): JsonRPCRequest {
        return JsonRPCRequest(
            method = "sendrawtransaction",
            params = listOf(signedTransaction),
            id = "id",
        )
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