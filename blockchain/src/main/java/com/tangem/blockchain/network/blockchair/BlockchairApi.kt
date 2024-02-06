package com.tangem.blockchain.network.blockchair

import com.squareup.moshi.JsonClass
import com.tangem.blockchain.network.blockchair.response.SendTransactionResponse
import retrofit2.http.*

interface BlockchairApi {
    @GET("dashboards/address/{address}")
    suspend fun getAddressData(
        @Path("address") address: String,
        @Query("transaction_details") transactionDetails: Boolean = false,
        @Query("limit") limit: Int? = null,
        @Query("key") key: String?,
        @Header("authorizationToken") authorizationToken: String?,
    ): BlockchairAddress

    @GET("dashboards/transaction/{transaction}")
    suspend fun getTransaction(
        @Path("transaction") transactionHash: String,
        @Query("key") key: String?,
        @Header("authorizationToken") authorizationToken: String?,
    ): BlockchairTransaction

    @GET("stats")
    suspend fun getBlockchainStats(
        @Query("key") key: String?,
        @Header("authorizationToken") authorizationToken: String?,
    ): BlockchairStats

    @POST("push/transaction")
    suspend fun sendTransaction(
        @Body sendBody: BlockchairBody,
        @Query("key") key: String?,
        @Header("authorizationToken") authorizationToken: String?,
    ): SendTransactionResponse

    @GET("erc-20/{contract_address}/dashboards/address/{address}")
    suspend fun getTokenHolderData(
        @Path("address") address: String,
        @Path("contract_address") contractAddress: String,
        @Query("limit") limit: Int? = null,
        @Query("key") key: String?,
        @Header("authorizationToken") authorizationToken: String?,
    ): BlockchairTokenHolder

    @GET("dashboards/address/{address}")
    suspend fun findErc20Tokens(
        @Path("address") address: String,
        @Query("key") key: String?,
        @Query("erc_20") erc20: Boolean = true,
        @Header("authorizationToken") authorizationToken: String?,
    ): BlockchairTokensResponse
}

@JsonClass(generateAdapter = true)
data class BlockchairBody(val data: String)
