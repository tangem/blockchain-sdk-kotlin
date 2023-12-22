package com.tangem.blockchain.blockchains.cardano.network.rosetta

import com.tangem.blockchain.blockchains.cardano.network.rosetta.model.*
import retrofit2.http.Body
import retrofit2.http.POST

interface RosettaApi {
    @POST("account/balance")
    suspend fun getBalances(@Body body: RosettaAddressBody): RosettaBalanceResponse

    @POST("account/coins")
    suspend fun getCoins(@Body body: RosettaAddressBody): RosettaCoinsResponse

    @POST("construction/submit")
    suspend fun submitTransaction(@Body body: RosettaSubmitBody): RosettaSubmitResponse

//    @POST("/construction/preprocess")
//    suspend fun preprocessTransaction(@Body body: RosettaPreprocessBody): RosettaPreprocessResponse
//
//    @POST("/construction/metadata")
//    suspend fun getMetadata(@Body body: RosettaMetadataBody): RosettaMetadataResponse
//
//    @POST("/construction/payloads")
//    suspend fun getPayloads(@Body body: RosettaPayloadsBody): RosettaPayloadsResponse
//
//    @POST("/construction/combine")
//    suspend fun combineTransaction(@Body body: RosettaCombineBody): RosettaCombineResponse
}
