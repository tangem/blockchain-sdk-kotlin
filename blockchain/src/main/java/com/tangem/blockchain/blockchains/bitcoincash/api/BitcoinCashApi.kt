package com.tangem.blockchain.blockchains.bitcoincash.api

import com.tangem.blockchain.blockchains.bitcoincash.network.BitconCashGetFeeResponse
import com.tangem.blockchain.common.JsonRPCRequest
import com.tangem.blockchain.network.blockbook.network.responses.SendTransactionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

internal interface BitcoinCashApi {

    @Headers("Content-Type: application/json")
    @POST("/")
    suspend fun getFee(@Body request: JsonRPCRequest): Response<BitconCashGetFeeResponse>

    @Headers("Content-Type: application/json")
    @POST("/")
    suspend fun sendTransaction(@Body request: JsonRPCRequest): Response<SendTransactionResponse>
}