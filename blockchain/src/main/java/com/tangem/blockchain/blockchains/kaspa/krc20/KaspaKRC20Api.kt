package com.tangem.blockchain.blockchains.kaspa.krc20

import retrofit2.http.*

interface KaspaKRC20Api {
    @GET("krc20/address/{address}/token/{token}")
    suspend fun getBalance(@Path("address") address: String, @Path("token") token: String): KaspaKRC20BalanceResponse
}