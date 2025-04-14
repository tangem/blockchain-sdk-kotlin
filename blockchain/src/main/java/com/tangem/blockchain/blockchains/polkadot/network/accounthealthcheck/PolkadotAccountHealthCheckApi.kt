package com.tangem.blockchain.blockchains.polkadot.network.accounthealthcheck

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

internal interface PolkadotAccountHealthCheckApi {

    @Headers("Content-Type: application/json")
    @POST("api/v2/scan/extrinsics")
    suspend fun getExtrinsicList(
        @Body body: ExtrinsicsListBody,
    ): PolkadotAccountHealthCheckResponse<ExtrinsicListResponse>

    @Headers("Content-Type: application/json")
    @POST("api/scan/extrinsic")
    suspend fun getExtrinsicDetail(
        @Body body: ExtrinsicDetailBody,
    ): PolkadotAccountHealthCheckResponse<ExtrinsicDetailResponse>

    @Headers("Content-Type: application/json")
    @POST("api/v2/scan/search")
    suspend fun getAccountInfo(@Body body: AccountBody): PolkadotAccountHealthCheckResponse<AccountResponse>
}