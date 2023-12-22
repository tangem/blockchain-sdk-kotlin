package com.tangem.blockchain.blockchains.tezos.network

import com.squareup.moshi.Json
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
    val contents: List<TezosOperationContent>,
)

@JsonClass(generateAdapter = true)
data class TezosOperationContent(
    @Json(name = "kind") val kind: String,
    @Json(name = "source") val source: String,
    @Json(name = "fee") val fee: String,
    @Json(name = "counter") val counter: String,
    @Json(name = "gas_limit") val gasLimit: String,
    @Json(name = "storage_limit") val storageLimit: String,
    @Json(name = "public_key") val publicKey: String? = null,
    @Json(name = "destination") val destination: String? = null,
    @Json(name = "amount") val amount: String? = null,
)

@JsonClass(generateAdapter = true)
data class TezosPreapplyBody(
    val protocol: String,
    val branch: String,
    val contents: List<TezosOperationContent>,
    val signature: String,
)
