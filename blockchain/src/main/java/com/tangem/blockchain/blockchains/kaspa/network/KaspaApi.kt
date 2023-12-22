package com.tangem.blockchain.blockchains.kaspa.network

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
}

@JsonClass(generateAdapter = true)
data class KaspaTransactionBody(
    val transaction: KaspaTransactionData,
)

@JsonClass(generateAdapter = true)
data class KaspaTransactionData(
    val version: Int = 0,
    val inputs: List<KaspaInput>,
    val outputs: List<KaspaOutput>,
    val lockTime: Int = 0,
    val subnetworkId: String = "0000000000000000000000000000000000000000",
)

@JsonClass(generateAdapter = true)
data class KaspaInput(
    val previousOutpoint: KaspaPreviousOutpoint,
    val signatureScript: String,
    val sequence: Long = 0,
    val sigOpCount: Int = 1,
)

@JsonClass(generateAdapter = true)
data class KaspaPreviousOutpoint(
    val transactionId: String,
    val index: Long,
)

@JsonClass(generateAdapter = true)
data class KaspaOutput(
    val amount: Long,
    val scriptPublicKey: KaspaScriptPublicKey,
)

@JsonClass(generateAdapter = true)
data class KaspaScriptPublicKey(
    val scriptPublicKey: String,
    val version: Int = 0,
)
