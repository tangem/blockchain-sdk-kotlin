package com.tangem.blockchain.blockchains.ton.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.UUID

interface TonApi {

    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST("jsonRPC")
    suspend fun post(@Body body: TonProviderRequestBody): ResponseBody
}

@JsonClass(generateAdapter = true)
data class TonProviderRequestBody(
    @Json(name = "method") val method: String,
    @Json(name = "params") val params: Map<String, Any>,
    @Json(name = "id") val id: String = UUID.randomUUID().toString(),
    @Json(name = "jsonrpc") val jsonRpc: String = "2.0",
)

sealed interface TonProviderMethod {

    fun asRequestBody(): TonProviderRequestBody

    data class GetWalletInformation(private val address: String) : TonProviderMethod {
        override fun asRequestBody(): TonProviderRequestBody = TonProviderRequestBody(
            method = "getWalletInformation",
            params = mapOf("address" to address),
        )
    }

    data class EstimateFee(private val address: String, private val body: String) : TonProviderMethod {
        override fun asRequestBody(): TonProviderRequestBody = TonProviderRequestBody(
            method = "estimateFee",
            params = mapOf(
                "address" to address,
                "body" to body,
            ),
        )
    }

    data class SendBocReturnHash(private val message: String) : TonProviderMethod {
        override fun asRequestBody(): TonProviderRequestBody = TonProviderRequestBody(
            method = "sendBocReturnHash",
            params = mapOf("boc" to message),
        )
    }

    data class RunGetMethod(
        private val contractAddress: String,
        private val method: String,
        private val stack: List<List<String>>
    ) : TonProviderMethod {
        override fun asRequestBody(): TonProviderRequestBody = TonProviderRequestBody(
            method = "runGetMethod",
            params = mapOf(
                "address" to contractAddress,
                "method" to method,
                "stack" to stack,
                // "method" to "get_wallet_address",
                // "stack" to listOf(listOf(
                //     "tvm.Slice",
                //     "TODO"
                // ))
            ),
        )
    }
}