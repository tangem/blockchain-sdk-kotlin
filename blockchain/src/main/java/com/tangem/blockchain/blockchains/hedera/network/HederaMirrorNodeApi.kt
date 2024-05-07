package com.tangem.blockchain.blockchains.hedera.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface HederaMirrorNodeApi {

    @GET("api/v1/accounts")
    suspend fun getAccountsByPublicKey(@Query("account.publickey") publicKey: String): HederaAccountResponse

    @GET("api/v1/network/exchangerate")
    suspend fun getExchangeRate(): HederaExchangeRateResponse

    @GET("api/v1/balances")
    suspend fun getBalances(@Query("account.id") accountId: String): HederaBalancesResponse

    @GET("api/v1/transactions/{transactionId}")
    suspend fun getTransactionInfo(@Path("transactionId") transactionId: String): HederaTransactionsResponse
}