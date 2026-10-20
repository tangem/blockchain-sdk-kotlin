package com.tangem.blockchain.blockchains.alephium.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

internal interface AlephiumApi {

    // Without the flag the node rejects addresses holding more than 5000 UTXOs
    @GET("addresses/{address}/utxos?error-if-exceed-max-utxos=false")
    suspend fun getUtxos(@Path("address") address: String): AlephiumResponse.Utxos

    @POST("transactions/build")
    suspend fun buildTx(@Body request: AlephiumRequest.BuildTx): AlephiumResponse.UnsignedTx

    @POST("transactions/submit")
    suspend fun submitTx(@Body request: AlephiumRequest.SubmitTx): AlephiumResponse.SubmitTx
}