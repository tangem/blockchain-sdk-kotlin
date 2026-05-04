package com.tangem.blockchain.blockchains.hedera.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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

    @GET("contracts/{contractIdOrAddress}")
    suspend fun getContractById(@Path("contractIdOrAddress") contractIdOrAddress: String): HederaContractResponse

    @POST("contracts/call")
    suspend fun invokeSmartContract(@Body body: HederaContractCallRequest): HederaContractCallResponse

    @GET("network/fees")
    suspend fun getNetworkFees(): HederaNetworkFeesResponse

    @GET("accounts/{idOrAliasOrEvmAddress}")
    suspend fun getAccountDetail(@Path("idOrAliasOrEvmAddress") id: String): HederaAccountDetailResponse

    private companion object {
        const val BALANCES_LIMIT = 200 // Arkhia doesn't support limit greater than 200
    }
}