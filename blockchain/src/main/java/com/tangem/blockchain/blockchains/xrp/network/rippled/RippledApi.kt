package com.tangem.blockchain.blockchains.xrp.network.rippled

import retrofit2.http.Body
import retrofit2.http.POST

interface RippledApi {
    @POST("./")
    suspend fun getAccount(@Body rippledBody: RippledBody): RippledAccountResponse

    @POST("./")
    suspend fun getServerState(@Body rippledBody: RippledBody = serverStateBody): RippledStateResponse

    @POST("./")
    suspend fun getFee(@Body rippledBody: RippledBody = feeBody): RippledFeeResponse

    @POST("./")
    suspend fun submitTransaction(@Body rippledBody: RippledBody): RippledSubmitResponse
}

enum class RippledMethod(val value: String) {
    ACCOUNT_INFO("account_info"),
    SERVER_STATE("server_state"),
    FEE("fee"),
    SUBMIT("submit"),
}

data class RippledBody(
    val method: String,
    val params: List<Map<String, String>> = listOf(),
)

val serverStateBody = RippledBody(RippledMethod.SERVER_STATE.value)
val feeBody = RippledBody(RippledMethod.FEE.value)
