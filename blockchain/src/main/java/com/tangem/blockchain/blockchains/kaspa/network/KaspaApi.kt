package com.tangem.blockchain.blockchains.kaspa.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.*

interface KaspaApi {
    @GET("addresses/{address}/balance")
    suspend fun getBalance(@Path("address") address: String): KaspaBalanceResponse

    @GET("addresses/{address}/utxos")
    suspend fun getUnspents(@Path("address") address: String): List<KaspaUnspentOutputResponse>

    @Headers("Content-Type: application/json")
    @POST("transactions")
    suspend fun sendTransaction(@Body transaction: KaspaTransactionBody): KaspaSendTransactionResponse

    @Headers("Content-Type: application/json")
    @POST("transactions/mass")
    suspend fun transactionMass(@Body transactionData: KaspaTransactionData): KaspaMassResponse

    @GET("info/fee-estimate")
    suspend fun getFeeEstimate(): KaspaFeeEstimateResponse
}

@JsonClass(generateAdapter = true)
data class KaspaTransactionBody(
    @Json(name = "transaction")
    val transaction: KaspaTransactionData,
)

@JsonClass(generateAdapter = true)
data class KaspaTransactionData(
    @Json(name = "version")
    val version: Int = 0,
    @Json(name = "inputs")
    val inputs: List<KaspaInput>,
    @Json(name = "outputs")
    val outputs: List<KaspaOutput>,
    @Json(name = "lockTime")
    val lockTime: Int = 0,
    @Json(name = "subnetworkId")
    val subnetworkId: String = "0000000000000000000000000000000000000000",
)

@JsonClass(generateAdapter = true)
data class KaspaInput(
    @Json(name = "previousOutpoint")
    val previousOutpoint: KaspaPreviousOutpoint,
    @Json(name = "signatureScript")
    val signatureScript: String,
    @Json(name = "sequence")
    val sequence: Long = 0,
    @Json(name = "sigOpCount")
    val sigOpCount: Int = 1,
)

@JsonClass(generateAdapter = true)
data class KaspaPreviousOutpoint(
    @Json(name = "transactionId")
    val transactionId: String,
    @Json(name = "index")
    val index: Long,
)

@JsonClass(generateAdapter = true)
data class KaspaOutput(
    @Json(name = "amount")
    val amount: Long,
    @Json(name = "scriptPublicKey")
    val scriptPublicKey: KaspaScriptPublicKey,
)

@JsonClass(generateAdapter = true)
data class KaspaScriptPublicKey(
    @Json(name = "scriptPublicKey")
    val scriptPublicKey: String,
    @Json(name = "version")
    val version: Int = 0,
)