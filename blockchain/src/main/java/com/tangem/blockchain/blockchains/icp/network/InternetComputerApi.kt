package com.tangem.blockchain.blockchains.icp.network

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import okhttp3.RequestBody

internal interface InternetComputerApi {

    @POST("api/v2/canister/{canister_id}/{request_type}")
    suspend fun makeRequest(
        @Path("canister_id") canister: String,
        @Path("request_type") requestType: String,
        @Body body: RequestBody,
        @Header("Accept") acceptType: String = X_CBOR_HEADER,
        @Header("Content-Type") contentType: String = X_CBOR_HEADER,
    )

    companion object {
        private const val X_CBOR_HEADER = "application/x-binary"
    }
}
