package com.tangem.blockchain.blockchains.ergo.network.api

import com.tangem.blockchain.blockchains.ergo.network.api.responses.ErgoApiAddress
import com.tangem.blockchain.blockchains.ergo.network.api.responses.ErgoApiBlockResponse
import com.tangem.blockchain.blockchains.ergo.network.api.responses.ErgoApiMempoolResponse
import com.tangem.blockchain.blockchains.ergo.network.api.responses.ErgoApiSendTransactionResponse
import com.tangem.blockchain.blockchains.ergo.network.api.responses.ErgoApiTransactionRespone
import com.tangem.blockchain.blockchains.ergo.network.api.responses.ErgoApiUnspentResponse
import retrofit2.http.*

interface ErgoApi {
    @Headers("Content-Type: application/json")
    @GET("/api/v1/addresses/{address}/balance/confirmed")
    suspend fun getAddressData(@Path("address") address: String): ErgoApiAddress

    @Headers("Content-Type: application/json")
    @GET("/api/v1/mempool/transactions/byAddress/{address}")
    suspend fun getMemPoolTransactions(@Path("address") address: String): ErgoApiMempoolResponse

    @Headers("Content-Type: application/json")
    @GET("/transactions/{transaction}")
    suspend fun getTransaction(@Path("transaction") transaction: String): ErgoApiTransactionRespone

    @Headers("Content-Type: application/json")
    @GET("/blocks")
    suspend fun getLastBlock(): ErgoApiBlockResponse

    @Headers("Content-Type: application/json")
    @GET("/transactions/boxes/byAddress/unspent/{address}")
    suspend fun getUnspent(@Path("address") address: String): List<ErgoApiUnspentResponse>

    @Headers("Content-Type: application/json")
    @POST("/api/v1/mempool/transactions/submit")
    suspend fun sendTransaction(@Body transaction: String): ErgoApiSendTransactionResponse
}
