package com.tangem.blockchain.blockchains.tezos.network

import com.squareup.moshi.JsonClass
import retrofit2.http.*

interface TezosApi {
    @GET("chains/main/blocks/head/context/contracts/{address}")
    suspend fun getAddressData(@Path("address") address: String): TezosAddressResponse

    @GET("chains/main/blocks/head/header")
    suspend fun getHeader(): TezosHeaderResponse

    @GET("chains/main/blocks/head/context/contracts/{address}/manager_key")
    suspend fun getManagerKey(@Path("address") address: String): String

    @Headers("Content-Type: application/json")
    @POST("chains/main/blocks/head/helpers/forge/operations")
    suspend fun forgeOperations(@Body tezosForgeBody: TezosForgeBody): String

    @Headers("Content-Type: application/json")
    @POST("chains/main/blocks/head/helpers/preapply/operations")
    suspend fun preapplyOperations(@Body tezosPreapplyBodyList: List<TezosPreapplyBody>)

    @Headers("Content-Type: application/json")
    @POST("injection/operation")
    suspend fun sendTransaction(@Body transaction: String)
}

@JsonClass(generateAdapter = true)
data class TezosForgeBody(
        val branch: String,
        val contents: List<TezosOperationContent>
)

@JsonClass(generateAdapter = true)
data class TezosOperationContent(
        val kind: String,
        val source: String,
        val fee: String,
        val counter: String,
        val gas_limit: String,
        val storage_limit: String,
        val public_key: String? = null,
        val destination: String? = null,
        val amount: String? = null
)

@JsonClass(generateAdapter = true)
data class TezosPreapplyBody(
        val protocol: String,
        val branch: String,
        val contents: List<TezosOperationContent>,
        val signature: String
)