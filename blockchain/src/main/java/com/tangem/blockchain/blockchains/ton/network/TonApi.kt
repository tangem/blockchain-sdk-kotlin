package com.tangem.blockchain.blockchains.ton.network

import com.tangem.blockchain.common.JsonRPCRequest
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

internal interface TonApi {

    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST("jsonRPC")
    suspend fun post(@Body body: JsonRPCRequest): ResponseBody
}

internal sealed interface TonProviderMethod {

    fun asRequestBody(): JsonRPCRequest

    data class GetWalletInformation(private val address: String) : TonProviderMethod {
        override fun asRequestBody(): JsonRPCRequest = JsonRPCRequest(
            method = "getWalletInformation",
            params = mapOf("address" to address),
        )
    }

    data class EstimateFee(private val address: String, private val body: String) : TonProviderMethod {
        override fun asRequestBody(): JsonRPCRequest = JsonRPCRequest(
            method = "estimateFee",
            params = mapOf(
                "address" to address,
                "body" to body,
            ),
        )
    }

    data class SendBocReturnHash(private val message: String) : TonProviderMethod {
        override fun asRequestBody(): JsonRPCRequest = JsonRPCRequest(
            method = "sendBocReturnHash",
            params = mapOf("boc" to message),
        )
    }

    data class RunGetMethod(
        private val contractAddress: String,
        private val method: String,
        private val stack: List<List<String>>,
    ) : TonProviderMethod {
        override fun asRequestBody(): JsonRPCRequest = JsonRPCRequest(
            method = "runGetMethod",
            params = mapOf(
                "address" to contractAddress,
                "method" to method,
                "stack" to stack,
            ),
        )
    }
}