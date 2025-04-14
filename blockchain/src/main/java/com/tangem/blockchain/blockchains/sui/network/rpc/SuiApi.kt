package com.tangem.blockchain.blockchains.sui.network.rpc

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

internal interface SuiApi {

    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST
    suspend fun post(@Url postfixUrl: String = "", @Body body: SuiJsonRpcRequestBody): ResponseBody
}