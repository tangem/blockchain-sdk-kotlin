package com.tangem.blockchain.blockchains.hedera.network

import retrofit2.http.GET
import retrofit2.http.Query

interface HederaMirrorNodeApi {
    @GET("api/v1/accounts")
    suspend fun getAccountsByPublicKey(@Query("account.publickey") publicKey: String): HederaAccountResponse

    @GET("api/v1/network/exchangerate")
    suspend fun getExchangeRate(): HederaExchangeRateResponse
}