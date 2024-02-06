package com.tangem.blockchain.blockchains.bitcoincash.apiservice

import com.tangem.blockchain.blockchains.bitcoincash.network.BitconCashGetFeeResponse
import com.tangem.blockchain.blockchains.bitcoincash.network.SendTransactionRequest
import com.tangem.blockchain.network.blockbook.network.requests.GetFeeRequest
import com.tangem.blockchain.network.blockbook.network.responses.SendTransactionResponse
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.Response

internal interface BitcoinCashApi {

    @Headers("Content-Type: application/json")
    @POST("/")
    suspend fun getFee(@Body request: GetFeeRequest): Response<BitconCashGetFeeResponse>

    @Headers("Content-Type: application/json")
    @POST("/")
    suspend fun sendTransaction(@Body request: SendTransactionRequest): Response<SendTransactionResponse>
}