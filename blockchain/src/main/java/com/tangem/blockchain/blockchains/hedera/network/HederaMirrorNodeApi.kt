package com.tangem.blockchain.blockchains.hedera.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface HederaMirrorNodeApi {

    @GET("accounts")
    suspend fun getAccountsByPublicKey(@Query("account.publickey") publicKey: String): HederaAccountResponse

    @GET("network/exchangerate")
    suspend fun getExchangeRate(): HederaExchangeRateResponse

    @GET("balances")
    suspend fun getBalances(
        @Query("account.id") accountId: String,
        @Query("limit") limit: Int = BALANCES_LIMIT,
    ): HederaBalancesResponse

    @GET("transactions/{transactionId}")
    suspend fun getTransactionInfo(@Path("transactionId") transactionId: String): HederaTransactionsResponse

    @GET("tokens/{tokenId}")
    suspend fun getTokenDetails(@Path("tokenId") tokenId: String): HederaTokenDetailsResponse

    private companion object {
        const val BALANCES_LIMIT = 200 // Arkhia doesn't support limit greater than 200
    }
}